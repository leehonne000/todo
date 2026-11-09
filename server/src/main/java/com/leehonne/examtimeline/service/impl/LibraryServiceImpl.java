package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.dto.UpdateEntryDTO;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.EntryImage;
import com.leehonne.examtimeline.entity.EntryTag;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.Tag;
import com.leehonne.examtimeline.enums.EntryType;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.EntryImageMapper;
import com.leehonne.examtimeline.mapper.EntryMapper;
import com.leehonne.examtimeline.mapper.EntryTagMapper;
import com.leehonne.examtimeline.mapper.SubjectMapper;
import com.leehonne.examtimeline.mapper.TagMapper;
import com.leehonne.examtimeline.service.LibraryService;
import com.leehonne.examtimeline.service.StorageService;
import com.leehonne.examtimeline.service.StorageService.ObjectStat;
import com.leehonne.examtimeline.vo.EntryDetailVO;
import com.leehonne.examtimeline.vo.EntryDetailVO.DetailImage;
import com.leehonne.examtimeline.vo.EntryListItemVO;
import com.leehonne.examtimeline.vo.TagVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * 自由库 + 标签业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper + selectPage 分页）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryServiceImpl implements LibraryService {

    private static final int MAX_IMAGES_PER_ENTRY = 10;

    private final EntryMapper entryMapper;
    private final SubjectMapper subjectMapper;
    private final TagMapper tagMapper;
    private final EntryTagMapper entryTagMapper;
    private final EntryImageMapper entryImageMapper;
    private final StorageService storage;

    @Override
    public PageVO<EntryListItemVO> list(Long userId, Long subjectId, Integer type,
                                        String tagIds, Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);

        // 类型筛选：只认 1/2，其余值视作无命中（与旧版内存过滤同语义）
        EntryType typeFilter = null;
        if (type != null) {
            if (type != 1 && type != 2) {
                return PageVO.of(p, s, 0, List.of());
            }
            typeFilter = EntryType.fromCode(type);
        }

        LambdaQueryWrapper<Entry> qw = new LambdaQueryWrapper<Entry>()
                .eq(Entry::getUserId, userId)
                .eq(subjectId != null, Entry::getSubjectId, subjectId)
                .eq(typeFilter != null, Entry::getType, typeFilter)
                .orderByDesc(Entry::getCreatedAt);

        // 标签筛选（逗号分隔 id，任一命中即可——标签查询入口语义）
        Set<Long> tagIdSet = parseTagIds(tagIds);
        if (!tagIdSet.isEmpty()) {
            Set<Long> hit = entryTagMapper.selectList(new LambdaQueryWrapper<EntryTag>()
                            .in(EntryTag::getTagId, tagIdSet)).stream()
                    .map(EntryTag::getEntryId)
                    .collect(Collectors.toSet());
            if (hit.isEmpty()) {
                return PageVO.of(p, s, 0, List.of());
            }
            qw.in(Entry::getId, hit);
        }

        IPage<Entry> pageResult = entryMapper.selectPage(new Page<>(p, s), qw);

        // 科目名批量取
        Set<Long> subjectIds = pageResult.getRecords().stream().map(Entry::getSubjectId).collect(Collectors.toSet());
        Map<Long, String> subjectNames = subjectNames(subjectIds);

        List<EntryListItemVO> items = pageResult.getRecords().stream()
                .map(e -> toListItem(e, subjectNames.get(e.getSubjectId())))
                .toList();
        return PageVO.of(p, s, pageResult.getTotal(), items);
    }

    @Override
    public EntryDetailVO detail(Long userId, Long entryId) {
        Entry entry = require(userId, entryId);
        return toDetail(entry, subjectNames(Set.of(entry.getSubjectId())).get(entry.getSubjectId()));
    }

    @Override
    @Transactional
    public void update(Long userId, Long entryId, UpdateEntryDTO req) {
        Entry entry = require(userId, entryId);

        String content = req.getContent() == null ? "" : req.getContent().trim();
        List<String> keys = req.getImageObjectKeys() == null ? List.of() : req.getImageObjectKeys();
        if (content.isEmpty() && keys.isEmpty()) {
            throw BusinessException.badRequest("文字与图片至少填一项");
        }
        if (keys.size() > MAX_IMAGES_PER_ENTRY) {
            throw BusinessException.badRequest("每条记录最多 10 张图片");
        }

        // 图片键校验：本记录已挂的键（保留）或本人 tmp 键（归档），其余不收
        Set<String> oldKeys = entryImageMapper.selectList(new LambdaQueryWrapper<EntryImage>()
                        .eq(EntryImage::getEntryId, entryId)
                        .orderByAsc(EntryImage::getSortOrder)).stream()
                .map(EntryImage::getObjectKey)
                .collect(Collectors.toSet());
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String key : keys) {
            if (key == null || !key.startsWith("user_" + userId + "/")) {
                throw BusinessException.badRequest("图片不属于当前用户");
            }
            if (!normalized.add(key)) {
                throw BusinessException.badRequest("同一张图片不能提交两次");
            }
            if (!oldKeys.contains(key) && !key.startsWith("user_" + userId + "/tmp/")) {
                throw BusinessException.badRequest("图片不属于当前记录，请重新上传");
            }
            if (!storage.exists(key)) {
                throw BusinessException.badRequest("图片不存在或已过期，请重新上传");
            }
        }

        // 文字（复习状态不因编辑改变）；content 可清空——UpdateWrapper 显式 set NULL
        entryMapper.update(null, new LambdaUpdateWrapper<Entry>()
                .eq(Entry::getId, entryId)
                .set(Entry::getContent, content.isEmpty() ? null : content)
                .set(Entry::getUpdatedAt, LocalDateTime.now()));

        // 标签整体替换
        entryTagMapper.delete(new LambdaQueryWrapper<EntryTag>().eq(EntryTag::getEntryId, entryId));
        linkTags(userId, entryId, req.getTagNames());

        // 图片整体替换：删掉没提交的、保留仍提交的（重排序）、归档新加的 tmp 键
        Map<String, EntryImage> oldByKey = new HashMap<>();
        for (EntryImage img : entryImageMapper.selectList(new LambdaQueryWrapper<EntryImage>()
                .eq(EntryImage::getEntryId, entryId)
                .orderByAsc(EntryImage::getSortOrder))) {
            oldByKey.put(img.getObjectKey(), img);
        }
        for (EntryImage old : oldByKey.values()) {
            if (!normalized.contains(old.getObjectKey())) {
                storage.delete(old.getObjectKey());
                entryImageMapper.deleteById(old.getId());
            }
        }
        int order = 0;
        for (String key : normalized) {
            EntryImage existing = oldByKey.get(key);
            if (existing != null) {
                if (existing.getSortOrder() != order) {
                    existing.setSortOrder(order);
                    entryImageMapper.updateById(existing);
                }
            } else {
                archiveOne(userId, entryId, key, order);
            }
            order++;
        }
    }

    @Override
    @Transactional
    public void delete(Long userId, Long entryId) {
        Entry entry = require(userId, entryId);
        // 先取对象键再删行；MinIO 清理失败不阻塞记录删除（对象孤儿可接受，日志留痕）
        List<String> keys = entryImageMapper.selectList(new LambdaQueryWrapper<EntryImage>()
                        .eq(EntryImage::getEntryId, entryId)
                        .orderByAsc(EntryImage::getSortOrder)).stream()
                .map(EntryImage::getObjectKey)
                .toList();
        entryTagMapper.delete(new LambdaQueryWrapper<EntryTag>().eq(EntryTag::getEntryId, entryId));
        entryImageMapper.delete(new LambdaQueryWrapper<EntryImage>().eq(EntryImage::getEntryId, entryId));
        entryMapper.deleteById(entry.getId());
        for (String key : keys) {
            try {
                storage.delete(key);
            } catch (Exception e) {
                log.warn("删除记录 {} 的图片对象 {} 失败：{}", entryId, key, e.getMessage());
            }
        }
    }

    @Override
    public List<TagVO> tags(Long userId, String q) {
        String kw = q == null ? "" : q.trim();
        return tagMapper.selectList(new LambdaQueryWrapper<Tag>()
                        .eq(Tag::getUserId, userId)
                        .like(!kw.isEmpty(), Tag::getName, kw)
                        .orderByAsc(Tag::getName)).stream()
                .map(t -> new TagVO(t.getId(), t.getName()))
                .toList();
    }

    // ---- 内部 ----

    private Entry require(Long userId, Long entryId) {
        Entry e = entryMapper.selectById(entryId);
        if (e == null || !e.getUserId().equals(userId)) {
            throw BusinessException.notFound("记录不存在");
        }
        return e;
    }

    private Set<Long> parseTagIds(String tagIds) {
        Set<Long> out = new LinkedHashSet<>();
        if (tagIds == null || tagIds.isBlank()) {
            return out;
        }
        for (String part : tagIds.split(",")) {
            String s = part.trim();
            if (s.isEmpty()) {
                continue;
            }
            try {
                out.add(Long.parseLong(s));
            } catch (NumberFormatException e) {
                throw BusinessException.badRequest("tagIds 格式错误（应为逗号分隔的数字 id）");
            }
        }
        return out;
    }

    private Map<Long, String> subjectNames(Set<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectMapper.selectBatchIds(subjectIds).stream()
                .collect(Collectors.toMap(Subject::getId, Subject::getName));
    }

    /** 标签名同名复用（与 Learn 落库同语义：t_tag 用户私有词表） */
    private void linkTags(Long userId, Long entryId, List<String> tagNames) {
        if (tagNames == null) {
            return;
        }
        Set<String> names = new LinkedHashSet<>();
        for (String name : tagNames) {
            if (name != null && !name.trim().isEmpty()) {
                String n = name.trim();
                if (n.length() > 32) {
                    throw BusinessException.badRequest("标签最长 32 字：" + n);
                }
                names.add(n);
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

    /** 一张新图归档：tmp 键 → user_{uid}/entry_{eid}/{文件名}（与 Learn Done 同语义） */
    private void archiveOne(Long userId, Long entryId, String tmpKey, int order) {
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
        img.setSortOrder(order);
        entryImageMapper.insert(img);
    }

    // ---- 组装（字段口径与前端 mock 的 entryListItem/entryDetail 对齐）----

    private EntryListItemVO toListItem(Entry e, String subjectName) {
        EntryListItemVO resp = new EntryListItemVO();
        resp.setId(e.getId());
        resp.setSubjectName(subjectName);
        resp.setType(e.getType().getCode());
        resp.setContent(e.getContent() == null ? "" : e.getContent());
        resp.setTags(tagNames(e.getId()));
        resp.setImageCount(entryImageMapper.selectCount(new LambdaQueryWrapper<EntryImage>()
                .eq(EntryImage::getEntryId, e.getId())).intValue());
        resp.setCreatedAt(e.getCreatedAt());
        resp.setRound(e.getRound());
        resp.setReviewStatus(e.getReviewStatus().getCode());
        return resp;
    }

    private EntryDetailVO toDetail(Entry e, String subjectName) {
        EntryDetailVO resp = new EntryDetailVO();
        resp.setId(e.getId());
        resp.setSubjectName(subjectName);
        resp.setType(e.getType().getCode());
        resp.setContent(e.getContent() == null ? "" : e.getContent());
        resp.setTags(tagNames(e.getId()));
        resp.setImages(entryImageMapper.selectList(new LambdaQueryWrapper<EntryImage>()
                        .eq(EntryImage::getEntryId, e.getId())
                        .orderByAsc(EntryImage::getSortOrder)).stream()
                .map(img -> new DetailImage(img.getObjectKey(), storage.signedUrl(img.getObjectKey())))
                .toList());
        resp.setCreatedAt(e.getCreatedAt());
        resp.setRound(e.getRound());
        resp.setReviewStatus(e.getReviewStatus().getCode());
        return resp;
    }

    /** 标签名按关联时间升序（= 添加顺序），与 ReviewService 同口径 */
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
}
