package com.leehonne.examtimeline.learn;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.EntryImage;
import com.leehonne.examtimeline.entity.EntryTag;
import com.leehonne.examtimeline.entity.StudySession;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.Tag;
import com.leehonne.examtimeline.enums.EntryType;
import com.leehonne.examtimeline.enums.ReviewStatus;
import com.leehonne.examtimeline.learn.dto.SaveEntriesRequest.EntryItemRequest;
import com.leehonne.examtimeline.learn.dto.StartSessionResponse;
import com.leehonne.examtimeline.learn.dto.SubjectResponse;
import com.leehonne.examtimeline.repository.EntryImageRepository;
import com.leehonne.examtimeline.repository.EntryRepository;
import com.leehonne.examtimeline.repository.EntryTagRepository;
import com.leehonne.examtimeline.repository.StudySessionRepository;
import com.leehonne.examtimeline.repository.SubjectRepository;
import com.leehonne.examtimeline.repository.TagRepository;
import com.leehonne.examtimeline.storage.MinioStorageService;
import com.leehonne.examtimeline.storage.MinioStorageService.ObjectStat;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Learn 流程业务（契约 §5）：选科目 → 计时 → 总结落库。
 * 每条记录入库即自动进艾宾浩斯队列（round=0，next_due_at=记录日+1 天），
 * 记录日 = 学习日（04:00 边界，见 {@link StudyDays}）。
 */
@Service
@RequiredArgsConstructor
public class LearnService {

    /** 一条记录最多 10 张图（契约 §4.4/§5.4） */
    private static final int MAX_IMAGES_PER_ENTRY = 10;

    private final SubjectRepository subjectRepository;
    private final StudySessionRepository sessionRepository;
    private final EntryRepository entryRepository;
    private final TagRepository tagRepository;
    private final EntryTagRepository entryTagRepository;
    private final EntryImageRepository entryImageRepository;
    private final MinioStorageService storage;

    /** 选科目（§5.1）：注册时定的科目组合，按展示顺序 */
    public List<SubjectResponse> subjects(Long userId) {
        return subjectRepository.findByUserIdOrderBySortOrderAsc(userId).stream()
                .map(s -> new SubjectResponse(s.getId(), s.getName(), s.getSortOrder()))
                .toList();
    }

    /** 开始计时（§5.2）：进横屏那一刻建行——保证计时中断也不丢统计 */
    public StartSessionResponse startSession(Long userId, Long subjectId) {
        Subject subject = subjectRepository.findByIdAndUserId(subjectId, userId)
                .orElseThrow(() -> BizException.badRequest("科目不存在"));
        StudySession session = new StudySession();
        session.setUserId(userId);
        session.setSubjectId(subject.getId());
        session.setStartedAt(LocalDateTime.now().withNano(0));
        sessionRepository.save(session);
        return new StartSessionResponse(session.getId(), session.getStartedAt());
    }

    /** 长按结束（§5.3）：写净学习秒数；异常中断没调也不影响已写值（duration 默认 0） */
    public void finishSession(Long userId, Long sessionId, Integer durationSeconds) {
        StudySession session = requireSession(userId, sessionId);
        session.setDurationSeconds(durationSeconds);
        session.setEndedAt(LocalDateTime.now().withNano(0));
        sessionRepository.save(session);
    }

    /**
     * 总结页 Done 批量落库（§5.4）：同事务写 t_entry + 标签关联 + 图片归档。
     * 每条自动进艾宾浩斯队列（round=0，next_due_at=记录日+1）；科目继承自 session。
     */
    @Transactional
    public List<Long> saveEntries(Long userId, Long sessionId, List<EntryItemRequest> items) {
        StudySession session = requireSession(userId, sessionId);

        // 先整批校验，任何一条不过整批不动（保证"同事务"语义）
        validateItems(userId, items);

        LocalDate recordDate = StudyDays.studyDate(LocalDateTime.now());
        List<Long> created = new ArrayList<>();
        for (EntryItemRequest item : items) {
            created.add(createOne(userId, session, item, recordDate));
        }
        return created;
    }

    // ---- 校验 ----

    private void validateItems(Long userId, List<EntryItemRequest> items) {
        Set<String> usedKeys = new LinkedHashSet<>();
        for (EntryItemRequest item : items) {
            if (item.getType() == null || (item.getType() != 1 && item.getType() != 2)) {
                throw BizException.badRequest("type 只能是 1（note）或 2（错题）");
            }
            String content = item.getContent() == null ? "" : item.getContent().trim();
            List<String> keys = item.getImageObjectKeys() == null ? List.of() : item.getImageObjectKeys();
            if (keys.size() > MAX_IMAGES_PER_ENTRY) {
                throw BizException.badRequest("每条记录最多 10 张图片");
            }
            if (content.isEmpty() && keys.isEmpty()) {
                throw BizException.badRequest("文字与图片至少填一项");
            }
            for (String key : keys) {
                if (key == null || !key.startsWith("user_" + userId + "/")) {
                    throw BizException.badRequest("图片不属于当前用户");
                }
                if (!usedKeys.add(key)) {
                    throw BizException.badRequest("同一张图片不能提交两次");
                }
                if (!storage.exists(key)) {
                    throw BizException.badRequest("图片不存在或已过期，请重新上传");
                }
            }
            if (item.getTagNames() != null) {
                for (String name : item.getTagNames()) {
                    if (name != null && name.trim().length() > 32) {
                        throw BizException.badRequest("标签最长 32 字：" + name.trim());
                    }
                }
            }
        }
    }

    // ---- 落库 ----

    private Long createOne(Long userId, StudySession session, EntryItemRequest item, LocalDate recordDate) {
        Entry entry = new Entry();
        entry.setSessionId(session.getId());
        entry.setUserId(userId);
        // 科目继承自 session（§5.4：前端不用传）
        entry.setSubjectId(session.getSubjectId());
        entry.setType(EntryType.fromCode(item.getType()));
        String content = item.getContent() == null ? "" : item.getContent().trim();
        entry.setContent(content.isEmpty() ? null : content);
        // 艾宾浩斯队列：入库 round=0，记录日+1 天首次到期（§6.1）
        entry.setRound(0);
        entry.setReviewStatus(ReviewStatus.QUEUED);
        entry.setNextDueAt(StudyDays.dueDate(recordDate, 0));
        entryRepository.save(entry);

        linkTags(userId, entry.getId(), item.getTagNames());
        archiveImages(userId, entry.getId(), item.getImageObjectKeys());
        return entry.getId();
    }

    /** 标签名同名复用（t_tag 是用户私有词表，唯一键 user_id+name） */
    private void linkTags(Long userId, Long entryId, List<String> tagNames) {
        if (tagNames == null) {
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        for (String name : tagNames) {
            if (name != null && !name.trim().isEmpty()) {
                names.add(name.trim());
            }
        }
        for (String name : names) {
            Tag tag = tagRepository.findByUserIdAndName(userId, name).orElseGet(() -> {
                Tag t = new Tag();
                t.setUserId(userId);
                t.setName(name);
                return tagRepository.save(t);
            });
            EntryTag link = new EntryTag();
            link.setEntryId(entryId);
            link.setTagId(tag.getId());
            entryTagRepository.save(link);
        }
    }

    /** 临时键归档到条目名下（user_{uid}/entry_{eid}/{文件名}），元数据落 t_entry_image */
    private void archiveImages(Long userId, Long entryId, List<String> imageObjectKeys) {
        if (imageObjectKeys == null) {
            return;
        }
        for (int i = 0; i < imageObjectKeys.size(); i++) {
            String tmpKey = imageObjectKeys.get(i);
            ObjectStat st = storage.stat(tmpKey);
            String fileName = tmpKey.substring(tmpKey.lastIndexOf('/') + 1);
            String targetKey = "user_" + userId + "/entry_" + entryId + "/" + fileName;
            storage.move(tmpKey, targetKey);

            EntryImage img = new EntryImage();
            img.setEntryId(entryId);
            img.setUserId(userId);
            img.setObjectKey(targetKey);
            img.setOriginalName(st.originalName());
            img.setContentType(st.contentType() != null ? st.contentType() : "image/webp");
            long size = st.sizeBytes();
            img.setSizeBytes(size > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) size);
            img.setSortOrder(i);
            entryImageRepository.save(img);
        }
    }

    private StudySession requireSession(Long userId, Long sessionId) {
        return sessionRepository.findById(sessionId)
                .filter(s -> s.getUserId().equals(userId))
                .orElseThrow(() -> BizException.notFound("会话不存在"));
    }
}
