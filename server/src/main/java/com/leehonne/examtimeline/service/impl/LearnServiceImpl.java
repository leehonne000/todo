package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.dto.SaveEntriesDTO.EntryItemDTO;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.EntryImage;
import com.leehonne.examtimeline.entity.EntryTag;
import com.leehonne.examtimeline.entity.StudySession;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.Tag;
import com.leehonne.examtimeline.enums.EntryType;
import com.leehonne.examtimeline.enums.ReviewStatus;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.EntryImageMapper;
import com.leehonne.examtimeline.mapper.EntryMapper;
import com.leehonne.examtimeline.mapper.EntryTagMapper;
import com.leehonne.examtimeline.mapper.StudySessionMapper;
import com.leehonne.examtimeline.mapper.SubjectMapper;
import com.leehonne.examtimeline.mapper.TagMapper;
import com.leehonne.examtimeline.service.LearnService;
import com.leehonne.examtimeline.service.StorageService;
import com.leehonne.examtimeline.service.StorageService.ObjectStat;
import com.leehonne.examtimeline.vo.StartSessionVO;
import com.leehonne.examtimeline.vo.SubjectVO;
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
 * Learn 流程业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper）。
 */
@Service
@RequiredArgsConstructor
public class LearnServiceImpl implements LearnService {

    /** 一条记录最多 10 张图（契约 §4.4/§5.4） */
    private static final int MAX_IMAGES_PER_ENTRY = 10;

    private final SubjectMapper subjectMapper;
    private final StudySessionMapper sessionMapper;
    private final EntryMapper entryMapper;
    private final TagMapper tagMapper;
    private final EntryTagMapper entryTagMapper;
    private final EntryImageMapper entryImageMapper;
    private final StorageService storage;

    @Override
    public List<SubjectVO> subjects(Long userId) {
        return subjectMapper.selectList(new LambdaQueryWrapper<Subject>()
                        .eq(Subject::getUserId, userId)
                        .orderByAsc(Subject::getSortOrder)).stream()
                .map(s -> new SubjectVO(s.getId(), s.getName(), s.getSortOrder()))
                .toList();
    }

    @Override
    public StartSessionVO startSession(Long userId, Long subjectId) {
        Subject subject = subjectMapper.selectOne(new LambdaQueryWrapper<Subject>()
                .eq(Subject::getId, subjectId)
                .eq(Subject::getUserId, userId));
        if (subject == null) {
            throw BusinessException.badRequest("科目不存在");
        }
        StudySession session = new StudySession();
        session.setUserId(userId);
        session.setSubjectId(subject.getId());
        session.setStartedAt(LocalDateTime.now().withNano(0));
        sessionMapper.insert(session);
        return new StartSessionVO(session.getId(), session.getStartedAt());
    }

    @Override
    public void finishSession(Long userId, Long sessionId, Integer durationSeconds) {
        StudySession session = requireSession(userId, sessionId);
        session.setDurationSeconds(durationSeconds);
        session.setEndedAt(LocalDateTime.now().withNano(0));
        sessionMapper.updateById(session);
    }

    @Override
    @Transactional
    public List<Long> saveEntries(Long userId, Long sessionId, List<EntryItemDTO> items) {
        StudySession session = requireSession(userId, sessionId);

        // 先整批校验，任何一条不过整批不动（保证"同事务"语义）
        validateItems(userId, items);

        LocalDate recordDate = StudyDays.studyDate(LocalDateTime.now());
        List<Long> created = new ArrayList<>();
        for (EntryItemDTO item : items) {
            created.add(createOne(userId, session, item, recordDate));
        }
        return created;
    }

    // ---- 校验 ----

    private void validateItems(Long userId, List<EntryItemDTO> items) {
        Set<String> usedKeys = new LinkedHashSet<>();
        for (EntryItemDTO item : items) {
            if (item.getType() == null || (item.getType() != 1 && item.getType() != 2)) {
                throw BusinessException.badRequest("type 只能是 1（note）或 2（错题）");
            }
            String content = item.getContent() == null ? "" : item.getContent().trim();
            List<String> keys = item.getImageObjectKeys() == null ? List.of() : item.getImageObjectKeys();
            if (keys.size() > MAX_IMAGES_PER_ENTRY) {
                throw BusinessException.badRequest("每条记录最多 10 张图片");
            }
            if (content.isEmpty() && keys.isEmpty()) {
                throw BusinessException.badRequest("文字与图片至少填一项");
            }
            for (String key : keys) {
                if (key == null || !key.startsWith("user_" + userId + "/")) {
                    throw BusinessException.badRequest("图片不属于当前用户");
                }
                if (!usedKeys.add(key)) {
                    throw BusinessException.badRequest("同一张图片不能提交两次");
                }
                if (!storage.exists(key)) {
                    throw BusinessException.badRequest("图片不存在或已过期，请重新上传");
                }
            }
            if (item.getTagNames() != null) {
                for (String name : item.getTagNames()) {
                    if (name != null && name.trim().length() > 32) {
                        throw BusinessException.badRequest("标签最长 32 字：" + name.trim());
                    }
                }
            }
        }
    }

    // ---- 落库 ----

    private Long createOne(Long userId, StudySession session, EntryItemDTO item, LocalDate recordDate) {
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
        entryMapper.insert(entry);

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
            Tag tag = tagMapper.selectOne(new LambdaQueryWrapper<Tag>()
                    .eq(Tag::getUserId, userId)
                    .eq(Tag::getName, name));
            if (tag == null) {
                tag = new Tag();
                tag.setUserId(userId);
                tag.setName(name);
                tagMapper.insert(tag);
            }
            EntryTag link = new EntryTag();
            link.setEntryId(entryId);
            link.setTagId(tag.getId());
            entryTagMapper.insert(link);
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
            entryImageMapper.insert(img);
        }
    }

    private StudySession requireSession(Long userId, Long sessionId) {
        StudySession s = sessionMapper.selectById(sessionId);
        if (s == null || !s.getUserId().equals(userId)) {
            throw BusinessException.notFound("会话不存在");
        }
        return s;
    }
}
