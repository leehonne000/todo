package com.leehonne.examtimeline.admin;

import com.leehonne.examtimeline.admin.dto.*;
import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.entity.Article;
import com.leehonne.examtimeline.entity.ExamMilestone;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.enums.ArticleStatus;
import com.leehonne.examtimeline.enums.UserRole;
import com.leehonne.examtimeline.repository.ArticleRepository;
import com.leehonne.examtimeline.repository.ExamMilestoneRepository;
import com.leehonne.examtimeline.repository.UserRepository;
import com.leehonne.examtimeline.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * 管理后台业务（契约 §11）——文章管理 + 用户管理 + 硬节点维护。
 *
 * <p>口径（2026-10-27 三级权限制）：
 * <ul>
 *   <li><b>角色三级</b>：0=普通用户 1=管理员 2=超级管理员（admin@bigtree.com，部署者 SQL 指定）。
 *       权限卡口在应用层，数据库不做独立权限管理。角色叠加——管理员同时拥有全部普通用户权限。</li>
 *   <li><b>文章归属权</b>：所有管理员可看全部文章（列表标作者），但只有<b>作者本人或超级管理员</b>能
 *       编辑/发布/下线/删除；管理员之间互不干涉。</li>
 *   <li><b>用户管理（任免）与硬节点维护仅超级管理员</b>：管理员之间不能取消对方权限；
 *       超管也不能取消自己的（防锁死）；超管身份不可由接口变更（只在 0/1 之间任免）。</li>
 *   <li><b>文章封面链路（MinIO 同步）</b>：封面先走 §5.5 上传拿临时键 {@code user_{uid}/tmp/...}，
 *       保存时归档到 {@code user_{uid}/article_{aid}/{文件名}}（与 Learn 图片同款 move=copy+删源）；
 *       编辑整体替换式——空=清封面、原样回传=不变、新键=换封面并清旧对象；删除文章物理删行并清封面对象。</li>
 *   <li><b>publishedAt = 首次发布时间</b>：只在草稿→已发布时写入，之后（含下线再发布）保持不变。</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminService {

    private final UserRepository userRepository;
    private final ArticleRepository articleRepository;
    private final ExamMilestoneRepository milestoneRepository;
    private final MinioStorageService storage;

    // ================= 权限卡口（§11.2） =================

    /** 管理后台入口校验：普通用户（role=0）抛 2002；管理员与超管都可进 */
    public User requireAdmin(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> BizException.unauthorized("用户不存在"));
        if (!u.getRole().isAtLeastAdmin()) {
            throw BizException.forbidden("需要管理员权限");
        }
        return u;
    }

    /** 超管卡口：用户任免与硬节点维护仅超级管理员（role=2） */
    private User requireSuperAdmin(Long userId) {
        User u = requireAdmin(userId);
        if (u.getRole() != UserRole.SUPER_ADMIN) {
            throw BizException.forbidden("需要超级管理员权限");
        }
        return u;
    }

    /** 文章操作权：作者本人或超级管理员；其他管理员 2002（互不干涉） */
    private void requireArticleOperator(User operator, Article article) {
        if (operator.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (!article.getAuthorId().equals(operator.getId())) {
            throw BizException.forbidden("只能操作自己创建的文章");
        }
    }

    // ================= 文章管理（§11.1~11.6、§11.9） =================

    /** 文章列表（§11.1）：含草稿，updatedAt 倒序，status 可选过滤；标作者（全部管理员可见） */
    public AdminArticleListResponse articles(Long adminId, Integer status, Integer page, Integer size) {
        requireAdmin(adminId);
        List<Article> all = new ArrayList<>(articleRepository.findAll(Sort.by(Sort.Direction.DESC, "updatedAt")));
        if (status != null) {
            if (status != 0 && status != 1) {
                throw BizException.badRequest("status 只能是 0（草稿）或 1（已发布）");
            }
            ArticleStatus want = ArticleStatus.fromCode(status);
            all.removeIf(a -> a.getStatus() != want);
        }

        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);
        int from = (p - 1) * s;
        List<Article> slice = from >= all.size() ? List.of()
                : all.subList(from, Math.min(from + s, all.size()));

        List<AdminArticleListItemResponse> items = slice.stream()
                .map(a -> new AdminArticleListItemResponse(a.getId(), a.getTitle(),
                        a.getStatus().getCode(), authorName(a.getAuthorId()), a.getAuthorId(),
                        a.getPublishedAt(), a.getUpdatedAt()))
                .toList();
        return new AdminArticleListResponse(items, all.size(), p, s);
    }

    /** 新建文章（§11.2）：一律先存草稿 */
    @Transactional
    public AdminArticleIdResponse create(Long adminId, SaveArticleRequest req) {
        requireAdmin(adminId);
        validateArticle(req);

        Article a = new Article();
        a.setAuthorId(adminId);
        a.setTitle(req.getTitle().trim());
        a.setContent(req.getContent());
        a.setStatus(ArticleStatus.DRAFT);
        articleRepository.saveAndFlush(a); // 先拿 id，封面才能归档到文章名下

        syncCover(adminId, a, req.getCoverObjectKey());
        articleRepository.save(a);
        return new AdminArticleIdResponse(a.getId());
    }

    /** 文章详情（§11.9 增补）：编辑表单回显，含草稿、含 coverObjectKey（原样回传=封面不变）；标作者与可操作标记 */
    public AdminArticleDetailResponse detail(Long adminId, Long articleId) {
        User op = requireAdmin(adminId);
        Article a = get(articleId);
        boolean editable = op.getRole() == UserRole.SUPER_ADMIN || a.getAuthorId().equals(op.getId());
        return new AdminArticleDetailResponse(a.getId(), a.getTitle(), a.getContent(),
                a.getCoverObjectKey(), coverUrl(a), a.getStatus().getCode(),
                authorName(a.getAuthorId()), a.getAuthorId(), editable,
                a.getPublishedAt(), a.getUpdatedAt());
    }

    /** 编辑文章（§11.3）：标题/正文/封面整体替换式；仅作者本人或超管 */
    @Transactional
    public void update(Long adminId, Long articleId, SaveArticleRequest req) {
        User op = requireAdmin(adminId);
        validateArticle(req);
        Article a = get(articleId);
        requireArticleOperator(op, a);
        a.setTitle(req.getTitle().trim());
        a.setContent(req.getContent());
        syncCover(adminId, a, req.getCoverObjectKey());
        articleRepository.save(a);
    }

    /** 发布（§11.4）：草稿→已发布；publishedAt 只在首次发布写入；仅作者本人或超管 */
    @Transactional
    public void publish(Long adminId, Long articleId) {
        User op = requireAdmin(adminId);
        Article a = get(articleId);
        requireArticleOperator(op, a);
        if (a.getStatus() == ArticleStatus.PUBLISHED) {
            return; // 幂等
        }
        a.setStatus(ArticleStatus.PUBLISHED);
        if (a.getPublishedAt() == null) {
            a.setPublishedAt(LocalDateTime.now());
        }
        articleRepository.save(a);
    }

    /** 下线（§11.5）：已发布→草稿（软控制，保留文章行与首次发布时间，可再次发布）；仅作者本人或超管 */
    @Transactional
    public void unpublish(Long adminId, Long articleId) {
        User op = requireAdmin(adminId);
        Article a = get(articleId);
        requireArticleOperator(op, a);
        if (a.getStatus() == ArticleStatus.DRAFT) {
            return; // 幂等
        }
        a.setStatus(ArticleStatus.DRAFT);
        articleRepository.save(a);
    }

    /** 删除文章（§11.6）：物理删行，封面对象一并清理；仅作者本人或超管 */
    @Transactional
    public void delete(Long adminId, Long articleId) {
        User op = requireAdmin(adminId);
        Article a = get(articleId);
        requireArticleOperator(op, a);
        articleRepository.delete(a);
        deleteQuietly(a.getCoverObjectKey());
    }

    // ================= 用户管理（§11.7、§11.8，仅超级管理员） =================

    /** 查用户（§11.7）：keyword 按 id/邮箱/昵称定位（任免入口），createdAt 倒序；仅超管 */
    public AdminUserListResponse users(Long adminId, String keyword, Integer page, Integer size) {
        requireSuperAdmin(adminId);
        List<User> all = userRepository.findAll(Sort.by(Sort.Direction.DESC, "createdAt"));
        String kw = keyword == null ? "" : keyword.trim();
        if (!kw.isEmpty()) {
            String lower = kw.toLowerCase(Locale.ROOT);
            Long asId = null;
            try {
                asId = Long.parseLong(kw);
            } catch (NumberFormatException ignored) {
                // 不是纯数字 → 只按文本匹配
            }
            final Long idHit = asId;
            all = all.stream().filter(u ->
                    (u.getEmail() != null && u.getEmail().toLowerCase(Locale.ROOT).contains(lower))
                            || (u.getNickname() != null && u.getNickname().toLowerCase(Locale.ROOT).contains(lower))
                            || (idHit != null && idHit.equals(u.getId()))
            ).toList();
        }

        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);
        int from = (p - 1) * s;
        List<User> slice = from >= all.size() ? List.of()
                : all.subList(from, Math.min(from + s, all.size()));

        List<AdminUserListItemResponse> items = slice.stream()
                .map(u -> new AdminUserListItemResponse(u.getId(), u.getEmail(), u.getNickname(),
                        u.getRole().getCode(), u.getExamYear(), u.getCreatedAt()))
                .toList();
        return new AdminUserListResponse(items, all.size(), p, s);
    }

    /**
     * 任免管理员（§11.8）：仅超级管理员可调；注册永远只产普通用户，管理员只能由此接口产生。
     * 规则：role 只在 0（取消）/1（提拔）之间变更——超级管理员身份由部署者 SQL 指定，接口不可变更；
     * 不能操作自己的账号（防锁死）；目标是超管时一律拒绝（超管不可被任免）。
     */
    @Transactional
    public AdminUserRoleResponse updateRole(Long adminId, Long targetId, Integer role) {
        requireSuperAdmin(adminId);
        if (role == null || (role != 0 && role != 1)) {
            throw BizException.badRequest("role 只能是 1（提拔为管理员）或 0（取消管理员）");
        }
        User target = userRepository.findById(targetId)
                .orElseThrow(() -> BizException.notFound("用户不存在"));
        if (targetId.equals(adminId)) {
            // 防误操作锁死后台：不能动自己的角色
            throw BizException.conflict("不能变更自己的角色，请让部署者操作");
        }
        if (target.getRole() == UserRole.SUPER_ADMIN) {
            throw BizException.conflict("超级管理员身份由部署者指定，不可在此变更");
        }
        target.setRole(UserRole.fromCode(role));
        userRepository.save(target);
        return new AdminUserRoleResponse(target.getId(), role);
    }

    // ================= 硬节点管理（§11.10~11.13 增补，仅超级管理员） =================

    /** 硬节点列表（§11.10）：examYear 可选过滤；examYear 升序、sortOrder 升序；仅超管 */
    public AdminMilestoneListResponse milestones(Long adminId, Integer examYear) {
        requireSuperAdmin(adminId);
        List<ExamMilestone> list = examYear == null
                ? milestoneRepository.findAll(Sort.by(Sort.Direction.ASC, "examYear", "sortOrder"))
                : milestoneRepository.findByExamYearOrderBySortOrderAsc(examYear);
        return new AdminMilestoneListResponse(list.stream().map(this::toMilestone).toList());
    }

    /** 新建硬节点（§11.11）；仅超管 */
    @Transactional
    public AdminMilestoneResponse createMilestone(Long adminId, SaveMilestoneRequest req) {
        requireSuperAdmin(adminId);
        validateMilestone(req);
        ExamMilestone m = new ExamMilestone();
        applyMilestone(m, req);
        return toMilestone(milestoneRepository.save(m));
    }

    /** 编辑硬节点（§11.12）：整体替换式；仅超管 */
    @Transactional
    public AdminMilestoneResponse updateMilestone(Long adminId, Long milestoneId, SaveMilestoneRequest req) {
        requireSuperAdmin(adminId);
        validateMilestone(req);
        ExamMilestone m = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> BizException.notFound("硬节点不存在"));
        applyMilestone(m, req);
        return toMilestone(milestoneRepository.save(m));
    }

    /** 删除硬节点（§11.13）；仅超管 */
    @Transactional
    public void deleteMilestone(Long adminId, Long milestoneId) {
        requireSuperAdmin(adminId);
        ExamMilestone m = milestoneRepository.findById(milestoneId)
                .orElseThrow(() -> BizException.notFound("硬节点不存在"));
        milestoneRepository.delete(m);
    }

    // ================= 内部 =================

    private Article get(Long articleId) {
        return articleRepository.findById(articleId)
                .orElseThrow(() -> BizException.notFound("文章不存在"));
    }

    /** 作者显示名：昵称优先，缺省回退邮箱 @ 前缀（与注册默认昵称口径一致） */
    private String authorName(Long authorId) {
        return userRepository.findById(authorId)
                .map(u -> u.getNickname() != null && !u.getNickname().isBlank()
                        ? u.getNickname()
                        : u.getEmail().substring(0, u.getEmail().indexOf('@')))
                .orElse("未知");
    }

    private void validateArticle(SaveArticleRequest req) {
        if (req == null || req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw BizException.badRequest("标题必填");
        }
        if (req.getTitle().trim().length() > 120) {
            throw BizException.badRequest("标题不能超过 120 字");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw BizException.badRequest("正文必填");
        }
    }

    /**
     * 封面整体替换式同步（MinIO 链路）：
     * 空 = 清封面（清旧对象）；与现键相同 = 不变；新键 = 归档到文章名下并清旧对象。
     * 新键一律走 move（copy+删源），从 tmp 临时键转正，与 Learn 图片归档同款。
     */
    private void syncCover(Long adminId, Article article, String incomingKey) {
        String old = article.getCoverObjectKey();
        String incoming = incomingKey == null ? "" : incomingKey.trim();

        if (incoming.isEmpty()) {
            if (old != null && !old.isBlank()) {
                deleteQuietly(old);
            }
            article.setCoverObjectKey(null);
            return;
        }
        if (incoming.equals(old)) {
            return; // 编辑表单原样回传旧键 = 封面不变
        }

        String archived = "user_" + adminId + "/article_" + article.getId() + "/" + fileName(incoming);
        if (!incoming.equals(archived)) {
            storage.move(incoming, archived); // 键不存在时抛 1001"图片不存在或已过期"
        }
        if (old != null && !old.isBlank() && !old.equals(archived)) {
            deleteQuietly(old);
        }
        article.setCoverObjectKey(archived);
    }

    /** 封面预签名 URL（管理端预览用），无封面为 null */
    private String coverUrl(Article a) {
        return a.getCoverObjectKey() == null || a.getCoverObjectKey().isBlank()
                ? null
                : storage.signedUrl(a.getCoverObjectKey());
    }

    /** 对象键 → 文件名 */
    private String fileName(String objectKey) {
        return objectKey.substring(objectKey.lastIndexOf('/') + 1);
    }

    /** 清对象（尽力而为）：清理失败不阻塞业务，只留日志 */
    private void deleteQuietly(String objectKey) {
        if (objectKey == null || objectKey.isBlank()) {
            return;
        }
        try {
            storage.delete(objectKey);
        } catch (Exception e) {
            log.warn("封面对象清理失败（忽略）key={}: {}", objectKey, e.getMessage());
        }
    }

    private void validateMilestone(SaveMilestoneRequest req) {
        if (req == null || req.getExamYear() == null || req.getExamYear() < 2000 || req.getExamYear() > 2100) {
            throw BizException.badRequest("届数 examYear 需在 2000~2100");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw BizException.badRequest("节点名必填");
        }
        if (req.getName().trim().length() > 32) {
            throw BizException.badRequest("节点名不能超过 32 字");
        }
        if (req.getDateFrom() == null) {
            throw BizException.badRequest("起始日 dateFrom 必填");
        }
        if (req.getDateTo() != null && req.getDateTo().isBefore(req.getDateFrom())) {
            throw BizException.badRequest("结束日不能早于起始日");
        }
    }

    private void applyMilestone(ExamMilestone m, SaveMilestoneRequest req) {
        m.setExamYear(req.getExamYear());
        m.setName(req.getName().trim());
        m.setDateFrom(req.getDateFrom());
        m.setDateTo(req.getDateTo());
        m.setSortOrder(req.getSortOrder() == null || req.getSortOrder() < 0 ? 0 : req.getSortOrder());
    }

    private AdminMilestoneResponse toMilestone(ExamMilestone m) {
        return new AdminMilestoneResponse(m.getId(), m.getExamYear(), m.getName(),
                m.getDateFrom(), m.getDateTo(), m.getSortOrder());
    }
}
