package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.dto.SaveArticleDTO;
import com.leehonne.examtimeline.dto.SaveMilestoneDTO;
import com.leehonne.examtimeline.entity.Article;
import com.leehonne.examtimeline.entity.ExamMilestone;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.enums.ArticleStatus;
import com.leehonne.examtimeline.enums.UserRole;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.ArticleMapper;
import com.leehonne.examtimeline.mapper.ExamMilestoneMapper;
import com.leehonne.examtimeline.mapper.UserMapper;
import com.leehonne.examtimeline.service.AdminService;
import com.leehonne.examtimeline.service.StorageService;
import com.leehonne.examtimeline.vo.AdminArticleDetailVO;
import com.leehonne.examtimeline.vo.AdminArticleIdVO;
import com.leehonne.examtimeline.vo.AdminArticleListItemVO;
import com.leehonne.examtimeline.vo.AdminMilestoneVO;
import com.leehonne.examtimeline.vo.AdminUserListItemVO;
import com.leehonne.examtimeline.vo.AdminUserRoleVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 管理后台业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper + selectPage 分页）。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminServiceImpl implements AdminService {

    private final UserMapper userMapper;
    private final ArticleMapper articleMapper;
    private final ExamMilestoneMapper milestoneMapper;
    private final StorageService storage;

    // ================= 权限卡口（§11.2） =================

    @Override
    public User requireAdmin(Long userId) {
        User u = userMapper.selectById(userId);
        if (u == null) {
            throw BusinessException.unauthorized("用户不存在");
        }
        if (!u.getRole().isAtLeastAdmin()) {
            throw BusinessException.forbidden("需要管理员权限");
        }
        return u;
    }

    /** 超管卡口：用户任免与硬节点维护仅超级管理员（role=2） */
    private User requireSuperAdmin(Long userId) {
        User u = requireAdmin(userId);
        if (u.getRole() != UserRole.SUPER_ADMIN) {
            throw BusinessException.forbidden("需要超级管理员权限");
        }
        return u;
    }

    /** 文章操作权：作者本人或超级管理员；其他管理员 2002（互不干涉） */
    private void requireArticleOperator(User operator, Article article) {
        if (operator.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (!article.getAuthorId().equals(operator.getId())) {
            throw BusinessException.forbidden("只能操作自己创建的文章");
        }
    }

    // ================= 文章管理（§11.1~11.6、§11.9） =================

    @Override
    public PageVO<AdminArticleListItemVO> articles(Long adminId, Integer status, Integer page, Integer size) {
        requireAdmin(adminId);
        if (status != null && status != 0 && status != 1) {
            throw BusinessException.badRequest("status 只能是 0（草稿）或 1（已发布）");
        }
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);

        IPage<Article> pageResult = articleMapper.selectPage(new Page<>(p, s),
                new LambdaQueryWrapper<Article>()
                        .eq(status != null, Article::getStatus,
                                status == null ? null : ArticleStatus.fromCode(status))
                        .orderByDesc(Article::getUpdatedAt));

        List<AdminArticleListItemVO> items = pageResult.getRecords().stream()
                .map(a -> new AdminArticleListItemVO(a.getId(), a.getTitle(),
                        a.getStatus().getCode(), authorName(a.getAuthorId()), a.getAuthorId(),
                        a.getPublishedAt(), a.getUpdatedAt()))
                .toList();
        return PageVO.of(p, s, pageResult.getTotal(), items);
    }

    @Override
    @Transactional
    public AdminArticleIdVO create(Long adminId, SaveArticleDTO req) {
        requireAdmin(adminId);
        validateArticle(req);

        Article a = new Article();
        a.setAuthorId(adminId);
        a.setTitle(req.getTitle().trim());
        a.setContent(req.getContent());
        a.setStatus(ArticleStatus.DRAFT);
        articleMapper.insert(a); // 先拿 id，封面才能归档到文章名下

        String coverKey = syncCover(adminId, a, req.getCoverObjectKey());
        if (coverKey != null) {
            a.setCoverObjectKey(coverKey);
            articleMapper.updateById(a);
        }
        return new AdminArticleIdVO(a.getId());
    }

    @Override
    public AdminArticleDetailVO detail(Long adminId, Long articleId) {
        User op = requireAdmin(adminId);
        Article a = get(articleId);
        boolean editable = op.getRole() == UserRole.SUPER_ADMIN || a.getAuthorId().equals(op.getId());
        return new AdminArticleDetailVO(a.getId(), a.getTitle(), a.getContent(),
                a.getCoverObjectKey(), coverUrl(a), a.getStatus().getCode(),
                authorName(a.getAuthorId()), a.getAuthorId(), editable,
                a.getPublishedAt(), a.getUpdatedAt());
    }

    @Override
    @Transactional
    public void update(Long adminId, Long articleId, SaveArticleDTO req) {
        User op = requireAdmin(adminId);
        validateArticle(req);
        Article a = get(articleId);
        requireArticleOperator(op, a);
        String coverKey = syncCover(adminId, a, req.getCoverObjectKey());
        // 封面可清空——UpdateWrapper 显式 set NULL
        articleMapper.update(null, new LambdaUpdateWrapper<Article>()
                .eq(Article::getId, articleId)
                .set(Article::getTitle, req.getTitle().trim())
                .set(Article::getContent, req.getContent())
                .set(Article::getCoverObjectKey, coverKey)
                .set(Article::getUpdatedAt, LocalDateTime.now()));
    }

    @Override
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
        articleMapper.updateById(a);
    }

    @Override
    @Transactional
    public void unpublish(Long adminId, Long articleId) {
        User op = requireAdmin(adminId);
        Article a = get(articleId);
        requireArticleOperator(op, a);
        if (a.getStatus() == ArticleStatus.DRAFT) {
            return; // 幂等
        }
        a.setStatus(ArticleStatus.DRAFT);
        articleMapper.updateById(a);
    }

    @Override
    @Transactional
    public void delete(Long adminId, Long articleId) {
        User op = requireAdmin(adminId);
        Article a = get(articleId);
        requireArticleOperator(op, a);
        articleMapper.deleteById(articleId);
        deleteQuietly(a.getCoverObjectKey());
    }

    // ================= 用户管理（§11.7、§11.8，仅超级管理员） =================

    @Override
    public PageVO<AdminUserListItemVO> users(Long adminId, String keyword, Integer page, Integer size) {
        requireSuperAdmin(adminId);
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);
        String kw = keyword == null ? "" : keyword.trim();

        LambdaQueryWrapper<User> qw = new LambdaQueryWrapper<User>()
                .orderByDesc(User::getCreatedAt);
        if (!kw.isEmpty()) {
            Long asId = null;
            try {
                asId = Long.parseLong(kw);
            } catch (NumberFormatException ignored) {
                // 不是纯数字 → 只按文本匹配
            }
            final Long idHit = asId;
            qw.and(w -> {
                w.like(User::getEmail, kw).or().like(User::getNickname, kw);
                if (idHit != null) {
                    w.or().eq(User::getId, idHit);
                }
            });
        }

        IPage<User> pageResult = userMapper.selectPage(new Page<>(p, s), qw);
        List<AdminUserListItemVO> items = pageResult.getRecords().stream()
                .map(u -> new AdminUserListItemVO(u.getId(), u.getEmail(), u.getNickname(),
                        u.getRole().getCode(), u.getExamYear(), u.getCreatedAt()))
                .toList();
        return PageVO.of(p, s, pageResult.getTotal(), items);
    }

    @Override
    @Transactional
    public AdminUserRoleVO updateRole(Long adminId, Long targetId, Integer role) {
        requireSuperAdmin(adminId);
        if (role == null || (role != 0 && role != 1)) {
            throw BusinessException.badRequest("role 只能是 1（提拔为管理员）或 0（取消管理员）");
        }
        User target = userMapper.selectById(targetId);
        if (target == null) {
            throw BusinessException.notFound("用户不存在");
        }
        if (targetId.equals(adminId)) {
            // 防误操作锁死后台：不能动自己的角色
            throw BusinessException.conflict("不能变更自己的角色，请让部署者操作");
        }
        if (target.getRole() == UserRole.SUPER_ADMIN) {
            throw BusinessException.conflict("超级管理员身份由部署者指定，不可在此变更");
        }
        target.setRole(UserRole.fromCode(role));
        userMapper.updateById(target);
        return new AdminUserRoleVO(target.getId(), role);
    }

    // ================= 硬节点管理（§11.10~11.13 增补，仅超级管理员） =================

    @Override
    public List<AdminMilestoneVO> milestones(Long adminId, Integer examYear) {
        requireSuperAdmin(adminId);
        return milestoneMapper.selectList(new LambdaQueryWrapper<ExamMilestone>()
                        .eq(examYear != null, ExamMilestone::getExamYear, examYear)
                        .orderByAsc(ExamMilestone::getExamYear)
                        .orderByAsc(ExamMilestone::getSortOrder)).stream()
                .map(this::toMilestone)
                .toList();
    }

    @Override
    @Transactional
    public AdminMilestoneVO createMilestone(Long adminId, SaveMilestoneDTO req) {
        requireSuperAdmin(adminId);
        validateMilestone(req);
        ExamMilestone m = new ExamMilestone();
        applyMilestone(m, req);
        milestoneMapper.insert(m);
        return toMilestone(m);
    }

    @Override
    @Transactional
    public AdminMilestoneVO updateMilestone(Long adminId, Long milestoneId, SaveMilestoneDTO req) {
        requireSuperAdmin(adminId);
        validateMilestone(req);
        ExamMilestone m = milestoneMapper.selectById(milestoneId);
        if (m == null) {
            throw BusinessException.notFound("硬节点不存在");
        }
        applyMilestone(m, req);
        milestoneMapper.updateById(m);
        return toMilestone(m);
    }

    @Override
    @Transactional
    public void deleteMilestone(Long adminId, Long milestoneId) {
        requireSuperAdmin(adminId);
        ExamMilestone m = milestoneMapper.selectById(milestoneId);
        if (m == null) {
            throw BusinessException.notFound("硬节点不存在");
        }
        milestoneMapper.deleteById(milestoneId);
    }

    // ================= 内部 =================

    private Article get(Long articleId) {
        Article a = articleMapper.selectById(articleId);
        if (a == null) {
            throw BusinessException.notFound("文章不存在");
        }
        return a;
    }

    /** 作者显示名：昵称优先，缺省回退邮箱 @ 前缀（与注册默认昵称口径一致） */
    private String authorName(Long authorId) {
        User u = userMapper.selectById(authorId);
        if (u == null) {
            return "未知";
        }
        return u.getNickname() != null && !u.getNickname().isBlank()
                ? u.getNickname()
                : u.getEmail().substring(0, u.getEmail().indexOf('@'));
    }

    private void validateArticle(SaveArticleDTO req) {
        if (req == null || req.getTitle() == null || req.getTitle().trim().isEmpty()) {
            throw BusinessException.badRequest("标题必填");
        }
        if (req.getTitle().trim().length() > 120) {
            throw BusinessException.badRequest("标题不能超过 120 字");
        }
        if (req.getContent() == null || req.getContent().isBlank()) {
            throw BusinessException.badRequest("正文必填");
        }
    }

    /**
     * 封面整体替换式同步（MinIO 链路）——只管对象与键计算，返回最终 coverObjectKey（可为 null=清封面）：
     * 空 = 清封面（清旧对象）；与现键相同 = 不变；新键 = 归档到文章名下并清旧对象。
     * 新键一律走 move（copy+删源），从 tmp 临时键转正，与 Learn 图片归档同款。
     */
    private String syncCover(Long adminId, Article article, String incomingKey) {
        String old = article.getCoverObjectKey();
        String incoming = incomingKey == null ? "" : incomingKey.trim();

        if (incoming.isEmpty()) {
            if (old != null && !old.isBlank()) {
                deleteQuietly(old);
            }
            return null;
        }
        if (incoming.equals(old)) {
            return old; // 编辑表单原样回传旧键 = 封面不变
        }

        String archived = "user_" + adminId + "/article_" + article.getId() + "/" + fileName(incoming);
        if (!incoming.equals(archived)) {
            storage.move(incoming, archived); // 键不存在时抛 1001"图片不存在或已过期"
        }
        if (old != null && !old.isBlank() && !old.equals(archived)) {
            deleteQuietly(old);
        }
        return archived;
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

    private void validateMilestone(SaveMilestoneDTO req) {
        if (req == null || req.getExamYear() == null || req.getExamYear() < 2000 || req.getExamYear() > 2100) {
            throw BusinessException.badRequest("届数 examYear 需在 2000~2100");
        }
        if (req.getName() == null || req.getName().trim().isEmpty()) {
            throw BusinessException.badRequest("节点名必填");
        }
        if (req.getName().trim().length() > 32) {
            throw BusinessException.badRequest("节点名不能超过 32 字");
        }
        if (req.getDateFrom() == null) {
            throw BusinessException.badRequest("起始日 dateFrom 必填");
        }
        if (req.getDateTo() != null && req.getDateTo().isBefore(req.getDateFrom())) {
            throw BusinessException.badRequest("结束日不能早于起始日");
        }
    }

    private void applyMilestone(ExamMilestone m, SaveMilestoneDTO req) {
        m.setExamYear(req.getExamYear());
        m.setName(req.getName().trim());
        m.setDateFrom(req.getDateFrom());
        m.setDateTo(req.getDateTo());
        m.setSortOrder(req.getSortOrder() == null || req.getSortOrder() < 0 ? 0 : req.getSortOrder());
    }

    private AdminMilestoneVO toMilestone(ExamMilestone m) {
        return new AdminMilestoneVO(m.getId(), m.getExamYear(), m.getName(),
                m.getDateFrom(), m.getDateTo(), m.getSortOrder());
    }
}
