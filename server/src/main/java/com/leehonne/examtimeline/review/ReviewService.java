package com.leehonne.examtimeline.review;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.EntryImage;
import com.leehonne.examtimeline.entity.EntryTag;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.Tag;
import com.leehonne.examtimeline.enums.ReviewStatus;
import com.leehonne.examtimeline.repository.EntryImageRepository;
import com.leehonne.examtimeline.repository.EntryRepository;
import com.leehonne.examtimeline.repository.EntryTagRepository;
import com.leehonne.examtimeline.repository.SubjectRepository;
import com.leehonne.examtimeline.repository.TagRepository;
import com.leehonne.examtimeline.review.dto.ReviewCompleteResponse;
import com.leehonne.examtimeline.review.dto.ReviewItemResponse;
import com.leehonne.examtimeline.review.dto.ReviewItemResponse.ReviewImage;
import com.leehonne.examtimeline.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Review 复习队列业务（契约 §6）——艾宾浩斯队列唯一消费入口。
 * 到期口径：review_status=0 AND next_due_at <= 今天（学习日）；
 * 复习完成 round+1 按节奏顺延，六轮走完出队（无自判/无重启）。
 */
@Service
@RequiredArgsConstructor
public class ReviewService {

    private final EntryRepository entryRepository;
    private final SubjectRepository subjectRepository;
    private final EntryTagRepository entryTagRepository;
    private final TagRepository tagRepository;
    private final EntryImageRepository entryImageRepository;
    private final MinioStorageService storage;

    /** 今日到期列表（§6.1）：跨科目堆叠，越早欠的排越前 */
    public List<ReviewItemResponse> due(Long userId) {
        LocalDate today = StudyDays.today();
        List<Entry> entries = new ArrayList<>(
                entryRepository.findByUserIdAndReviewStatusAndNextDueAtLessThanEqual(
                        userId, ReviewStatus.QUEUED, today));
        entries.sort(Comparator.comparing(Entry::getNextDueAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Set<Long> subjectIds = entries.stream().map(Entry::getSubjectId).collect(Collectors.toSet());
        Map<Long, String> subjectNames = subjectIds.isEmpty() ? Map.of()
                : subjectRepository.findAllById(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<ReviewItemResponse> out = new ArrayList<>();
        for (Entry e : entries) {
            out.add(toItem(e, subjectNames.get(e.getSubjectId())));
        }
        return out;
    }

    /**
     * 复习完成（§6.2）：无自判——完成该项即推进一轮。
     * round+1 后按 1/2/4/7/15/22（相对记录日）顺延；round=6 出队（review_status=1，next_due_at=null）。
     */
    @Transactional
    public ReviewCompleteResponse complete(Long userId, Long entryId) {
        Entry entry = entryRepository.findById(entryId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> BizException.notFound("记录不存在"));
        if (entry.getReviewStatus() == ReviewStatus.DONE) {
            throw BizException.conflict("该记录已完成全部复习，不在队列中");
        }

        int newRound = entry.getRound() + 1;
        if (newRound >= StudyDays.INTERVALS.length) {
            entry.setRound(StudyDays.INTERVALS.length);
            entry.setReviewStatus(ReviewStatus.DONE);
            entry.setNextDueAt(null);
        } else {
            // 相对记录日顺延（记录日 = 学习日）
            entry.setRound(newRound);
            entry.setNextDueAt(StudyDays.dueDate(StudyDays.studyDate(entry.getCreatedAt()), newRound));
        }
        entryRepository.save(entry);
        return new ReviewCompleteResponse(entry.getRound(), entry.getNextDueAt());
    }

    // ---- 组装 ----

    private ReviewItemResponse toItem(Entry e, String subjectName) {
        ReviewItemResponse resp = new ReviewItemResponse();
        resp.setId(e.getId());
        resp.setSubjectName(subjectName);
        resp.setType(e.getType().getCode());
        resp.setContent(e.getContent() == null ? "" : e.getContent());
        resp.setTags(tagNames(e.getId()));
        resp.setImages(images(e.getId()));
        resp.setCreatedAt(e.getCreatedAt());
        resp.setRound(e.getRound());
        resp.setReviewStatus(e.getReviewStatus().getCode());
        return resp;
    }

    /** 标签名按关联时间升序（= 添加顺序） */
    private List<String> tagNames(Long entryId) {
        List<EntryTag> links = new ArrayList<>(entryTagRepository.findByEntryId(entryId));
        if (links.isEmpty()) {
            return List.of();
        }
        links.sort(Comparator.comparing(EntryTag::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Set<Long> tagIds = links.stream().map(EntryTag::getTagId).collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> names = tagRepository.findAllById(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        return links.stream()
                .map(l -> names.get(l.getTagId()))
                .filter(n -> n != null)
                .toList();
    }

    private List<ReviewImage> images(Long entryId) {
        return entryImageRepository.findByEntryIdOrderBySortOrderAsc(entryId).stream()
                .map(img -> new ReviewImage(img.getObjectKey(), storage.signedUrl(img.getObjectKey())))
                .toList();
    }
}
