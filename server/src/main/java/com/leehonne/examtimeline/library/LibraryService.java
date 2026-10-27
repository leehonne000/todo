package com.leehonne.examtimeline.library;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.entity.Entry;
import com.leehonne.examtimeline.entity.EntryImage;
import com.leehonne.examtimeline.entity.EntryTag;
import com.leehonne.examtimeline.entity.Subject;
import com.leehonne.examtimeline.entity.Tag;
import com.leehonne.examtimeline.library.dto.EntryDetailResponse;
import com.leehonne.examtimeline.library.dto.EntryDetailResponse.DetailImage;
import com.leehonne.examtimeline.library.dto.EntryListItemResponse;
import com.leehonne.examtimeline.library.dto.EntryListResponse;
import com.leehonne.examtimeline.library.dto.TagResponse;
import com.leehonne.examtimeline.library.dto.UpdateEntryRequest;
import com.leehonne.examtimeline.repository.EntryImageRepository;
import com.leehonne.examtimeline.repository.EntryRepository;
import com.leehonne.examtimeline.repository.EntryTagRepository;
import com.leehonne.examtimeline.repository.SubjectRepository;
import com.leehonne.examtimeline.repository.TagRepository;
import com.leehonne.examtimeline.storage.MinioStorageService;
import com.leehonne.examtimeline.storage.MinioStorageService.ObjectStat;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
 * 自由库 + 标签业务（契约 §7）——复习模块「总览」本体（产品文档 §5.2）。
 *
 * <p>要点：
 * <ul>
 *   <li>列表/详情只看自己的记录；筛选（科目/类型/标签）后按 createdAt 倒序分页</li>
 *   <li>修改是<strong>整体替换</strong>（文字/标签/图片），但<strong>复习状态不因编辑改变</strong>（§7.3，不重启不顺延）</li>
 *   <li>删除=物理删除，级联清标签关联与图片元数据，MinIO 对象一并清（标签词表独立保留）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LibraryService {

    private static final int MAX_IMAGES_PER_ENTRY = 10;

    private final EntryRepository entryRepository;
    private final SubjectRepository subjectRepository;
    private final TagRepository tagRepository;
    private final EntryTagRepository entryTagRepository;
    private final EntryImageRepository entryImageRepository;
    private final MinioStorageService storage;

    /** 列表（§7.1）：自由库浏览与标签查询共用 */
    public EntryListResponse list(Long userId, Long subjectId, Integer type,
                                  String tagIds, Integer page, Integer size) {
        List<Entry> all = entryRepository.findByUserIdOrderByCreatedAtDesc(userId);

        // 科目 / 类型筛选
        List<Entry> filtered = all.stream()
                .filter(e -> subjectId == null || subjectId.equals(e.getSubjectId()))
                .filter(e -> type == null || type.equals(e.getType().getCode()))
                .collect(Collectors.toCollection(ArrayList::new));

        // 标签筛选（逗号分隔 id，任一命中即可——标签查询入口语义）
        Set<Long> tagIdSet = parseTagIds(tagIds);
        if (!tagIdSet.isEmpty()) {
            Set<Long> hit = new HashSet<>();
            for (Long tid : tagIdSet) {
                for (EntryTag l : entryTagRepository.findByTagId(tid)) {
                    hit.add(l.getEntryId());
                }
            }
            filtered = filtered.stream().filter(e -> hit.contains(e.getId())).toList();
        }

        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);
        int from = (p - 1) * s;
        List<Entry> slice = from >= filtered.size() ? List.of()
                : filtered.subList(from, Math.min(from + s, filtered.size()));

        // 科目名批量取
        Set<Long> subjectIds = filtered.stream().map(Entry::getSubjectId).collect(Collectors.toSet());
        Map<Long, String> subjectNames = subjectNames(subjectIds);

        List<EntryListItemResponse> items = new ArrayList<>();
        for (Entry e : slice) {
            items.add(toListItem(e, subjectNames.get(e.getSubjectId())));
        }
        return new EntryListResponse(items, filtered.size(), p, s);
    }

    /** 详情（§7.2） */
    public EntryDetailResponse detail(Long userId, Long entryId) {
        Entry entry = require(userId, entryId);
        return toDetail(entry, subjectNames(Set.of(entry.getSubjectId())).get(entry.getSubjectId()));
    }

    /**
     * 修改（§7.3）：整体替换式——content/tagNames/imageObjectKeys 按本次提交全量落。
     * 复习状态（round/next_due_at/review_status）一律不动。
     */
    @Transactional
    public void update(Long userId, Long entryId, UpdateEntryRequest req) {
        Entry entry = require(userId, entryId);

        String content = req.getContent() == null ? "" : req.getContent().trim();
        List<String> keys = req.getImageObjectKeys() == null ? List.of() : req.getImageObjectKeys();
        if (content.isEmpty() && keys.isEmpty()) {
            throw BizException.badRequest("文字与图片至少填一项");
        }
        if (keys.size() > MAX_IMAGES_PER_ENTRY) {
            throw BizException.badRequest("每条记录最多 10 张图片");
        }

        // 图片键校验：本记录已挂的键（保留）或本人 tmp 键（归档），其余不收
        Set<String> oldKeys = entryImageRepository.findByEntryIdOrderBySortOrderAsc(entryId).stream()
                .map(EntryImage::getObjectKey)
                .collect(Collectors.toSet());
        LinkedHashSet<String> normalized = new LinkedHashSet<>();
        for (String key : keys) {
            if (key == null || !key.startsWith("user_" + userId + "/")) {
                throw BizException.badRequest("图片不属于当前用户");
            }
            if (!normalized.add(key)) {
                throw BizException.badRequest("同一张图片不能提交两次");
            }
            if (!oldKeys.contains(key) && !key.startsWith("user_" + userId + "/tmp/")) {
                throw BizException.badRequest("图片不属于当前记录，请重新上传");
            }
            if (!storage.exists(key)) {
                throw BizException.badRequest("图片不存在或已过期，请重新上传");
            }
        }

        // 文字（复习状态不因编辑改变）
        entry.setContent(content.isEmpty() ? null : content);
        entryRepository.save(entry);

        // 标签整体替换
        entryTagRepository.deleteByEntryId(entryId);
        entryTagRepository.flush();
        linkTags(userId, entryId, req.getTagNames());

        // 图片整体替换：删掉没提交的、保留仍提交的（重排序）、归档新加的 tmp 键
        Map<String, EntryImage> oldByKey = new HashMap<>();
        for (EntryImage img : entryImageRepository.findByEntryIdOrderBySortOrderAsc(entryId)) {
            oldByKey.put(img.getObjectKey(), img);
        }
        for (EntryImage old : oldByKey.values()) {
            if (!normalized.contains(old.getObjectKey())) {
                storage.delete(old.getObjectKey());
                entryImageRepository.delete(old);
            }
        }
        int order = 0;
        for (String key : normalized) {
            EntryImage existing = oldByKey.get(key);
            if (existing != null) {
                if (existing.getSortOrder() != order) {
                    existing.setSortOrder(order);
                    entryImageRepository.save(existing);
                }
            } else {
                archiveOne(userId, entryId, key, order);
            }
            order++;
        }
    }

    /** 删除（§7.4）：物理删除，级联清关联与图片对象（标签词表独立保留） */
    @Transactional
    public void delete(Long userId, Long entryId) {
        Entry entry = require(userId, entryId);
        // 先取对象键再删行；MinIO 清理失败不阻塞记录删除（对象孤儿可接受，日志留痕）
        List<String> keys = entryImageRepository.findByEntryIdOrderBySortOrderAsc(entryId).stream()
                .map(EntryImage::getObjectKey)
                .toList();
        entryTagRepository.deleteByEntryId(entryId);
        entryImageRepository.deleteByEntryId(entryId);
        entryRepository.delete(entry);
        for (String key : keys) {
            try {
                storage.delete(key);
            } catch (Exception e) {
                log.warn("删除记录 {} 的图片对象 {} 失败：{}", entryId, key, e.getMessage());
            }
        }
    }

    /** 标签词表（§7.5）：按名称升序，q 模糊过滤 */
    public List<TagResponse> tags(Long userId, String q) {
        String kw = q == null ? "" : q.trim();
        return tagRepository.findByUserIdOrderByNameAsc(userId).stream()
                .filter(t -> kw.isEmpty() || t.getName().contains(kw))
                .map(t -> new TagResponse(t.getId(), t.getName()))
                .toList();
    }

    // ---- 内部 ----

    private Entry require(Long userId, Long entryId) {
        return entryRepository.findById(entryId)
                .filter(e -> e.getUserId().equals(userId))
                .orElseThrow(() -> BizException.notFound("记录不存在"));
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
                throw BizException.badRequest("tagIds 格式错误（应为逗号分隔的数字 id）");
            }
        }
        return out;
    }

    private Map<Long, String> subjectNames(Set<Long> subjectIds) {
        if (subjectIds.isEmpty()) {
            return Map.of();
        }
        return subjectRepository.findAllById(subjectIds).stream()
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
                    throw BizException.badRequest("标签最长 32 字：" + n);
                }
                names.add(n);
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
        entryImageRepository.save(img);
    }

    // ---- 组装（字段口径与前端 mock 的 entryListItem/entryDetail 对齐）----

    private EntryListItemResponse toListItem(Entry e, String subjectName) {
        EntryListItemResponse resp = new EntryListItemResponse();
        resp.setId(e.getId());
        resp.setSubjectName(subjectName);
        resp.setType(e.getType().getCode());
        resp.setContent(e.getContent() == null ? "" : e.getContent());
        resp.setTags(tagNames(e.getId()));
        resp.setImageCount(entryImageRepository.findByEntryIdOrderBySortOrderAsc(e.getId()).size());
        resp.setCreatedAt(e.getCreatedAt());
        resp.setRound(e.getRound());
        resp.setReviewStatus(e.getReviewStatus().getCode());
        return resp;
    }

    private EntryDetailResponse toDetail(Entry e, String subjectName) {
        EntryDetailResponse resp = new EntryDetailResponse();
        resp.setId(e.getId());
        resp.setSubjectName(subjectName);
        resp.setType(e.getType().getCode());
        resp.setContent(e.getContent() == null ? "" : e.getContent());
        resp.setTags(tagNames(e.getId()));
        resp.setImages(entryImageRepository.findByEntryIdOrderBySortOrderAsc(e.getId()).stream()
                .map(img -> new DetailImage(img.getObjectKey(), storage.signedUrl(img.getObjectKey())))
                .toList());
        resp.setCreatedAt(e.getCreatedAt());
        resp.setRound(e.getRound());
        resp.setReviewStatus(e.getReviewStatus().getCode());
        return resp;
    }

    /** 标签名按关联时间升序（= 添加顺序），与 ReviewService 同口径 */
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
}
