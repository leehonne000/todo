package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.EntryImage;
import com.leehonne.examtimeline.entity.EntryTag;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.Tag;
import com.leehonne.examtimeline.enums.ReviewStatus;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.EntryImageMapper;
import com.leehonne.examtimeline.mapper.EntryMapper;
import com.leehonne.examtimeline.mapper.EntryTagMapper;
import com.leehonne.examtimeline.mapper.SubjectMapper;
import com.leehonne.examtimeline.mapper.TagMapper;
import com.leehonne.examtimeline.service.ReviewService;
import com.leehonne.examtimeline.service.StorageService;
import com.leehonne.examtimeline.vo.ReviewCompleteVO;
import com.leehonne.examtimeline.vo.ReviewItemVO;
import com.leehonne.examtimeline.vo.ReviewItemVO.ReviewImage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Review 复习队列业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper）。
 */
@Service
@RequiredArgsConstructor
public class ReviewServiceImpl implements ReviewService {

    private final EntryMapper entryMapper;
    private final SubjectMapper subjectMapper;
    private final EntryTagMapper entryTagMapper;
    private final TagMapper tagMapper;
    private final EntryImageMapper entryImageMapper;
    private final StorageService storage;

    @Override
    public List<ReviewItemVO> due(Long userId) {
        LocalDate today = StudyDays.today();
        List<Entry> entries = new ArrayList<>(entryMapper.selectList(new LambdaQueryWrapper<Entry>()
                .eq(Entry::getUserId, userId)
                .eq(Entry::getReviewStatus, ReviewStatus.QUEUED)
                .le(Entry::getNextDueAt, today)));
        entries.sort(Comparator.comparing(Entry::getNextDueAt,
                Comparator.nullsLast(Comparator.naturalOrder())));

        Set<Long> subjectIds = entries.stream().map(Entry::getSubjectId).collect(Collectors.toSet());
        Map<Long, String> subjectNames = subjectIds.isEmpty() ? Map.of()
                : subjectMapper.selectBatchIds(subjectIds).stream()
                        .collect(Collectors.toMap(Subject::getId, Subject::getName));

        List<ReviewItemVO> out = new ArrayList<>();
        for (Entry e : entries) {
            out.add(toItem(e, subjectNames.get(e.getSubjectId())));
        }
        return out;
    }

    @Override
    @Transactional
    public ReviewCompleteVO complete(Long userId, Long entryId) {
        Entry entry = entryMapper.selectById(entryId);
        if (entry == null || !entry.getUserId().equals(userId)) {
            throw BusinessException.notFound("记录不存在");
        }
        if (entry.getReviewStatus() == ReviewStatus.DONE) {
            throw BusinessException.conflict("该记录已完成全部复习，不在队列中");
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
            entry.setReviewStatus(ReviewStatus.QUEUED);
        }
        // 出队分支 next_due_at 要显式写 NULL——updateById 不落 null，用 UpdateWrapper 显式 set
        entryMapper.update(null, new LambdaUpdateWrapper<Entry>()
                .eq(Entry::getId, entry.getId())
                .set(Entry::getRound, entry.getRound())
                .set(Entry::getReviewStatus, entry.getReviewStatus())
                .set(Entry::getNextDueAt, entry.getNextDueAt())
                .set(Entry::getUpdatedAt, LocalDateTime.now()));
        return new ReviewCompleteVO(entry.getRound(), entry.getNextDueAt());
    }

    // ---- 组装 ----

    private ReviewItemVO toItem(Entry e, String subjectName) {
        ReviewItemVO resp = new ReviewItemVO();
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
        List<EntryTag> links = new ArrayList<>(entryTagMapper.selectList(new LambdaQueryWrapper<EntryTag>()
                .eq(EntryTag::getEntryId, entryId)));
        if (links.isEmpty()) {
            return List.of();
        }
        links.sort(Comparator.comparing(EntryTag::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        Set<Long> tagIds = links.stream().map(EntryTag::getTagId).collect(Collectors.toCollection(HashSet::new));
        Map<Long, String> names = tagMapper.selectBatchIds(tagIds).stream()
                .collect(Collectors.toMap(Tag::getId, Tag::getName));
        return links.stream()
                .map(l -> names.get(l.getTagId()))
                .filter(n -> n != null)
                .toList();
    }

    private List<ReviewImage> images(Long entryId) {
        return entryImageMapper.selectList(new LambdaQueryWrapper<EntryImage>()
                        .eq(EntryImage::getEntryId, entryId)
                        .orderByAsc(EntryImage::getSortOrder)).stream()
                .map(img -> new ReviewImage(img.getObjectKey(), storage.signedUrl(img.getObjectKey())))
                .toList();
    }
}
