package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.dto.SaveArticleDTO;
import com.leehonne.examtimeline.dto.SaveMilestoneDTO;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.vo.AdminArticleDetailVO;
import com.leehonne.examtimeline.vo.AdminArticleIdVO;
import com.leehonne.examtimeline.vo.AdminArticleListItemVO;
import com.leehonne.examtimeline.vo.AdminMilestoneVO;
import com.leehonne.examtimeline.vo.AdminUserListItemVO;
import com.leehonne.examtimeline.vo.AdminUserRoleVO;

import java.util.List;

/**
 * 管理后台业务（契约 §11）——文章管理 + 用户管理 + 硬节点维护。
 *
 * <p>口径（2026-10-27 三级权限制）：
 * <ul>
 *   <li><b>角色三级</b>：0=普通用户 1=管理员 2=超级管理员（admin@bigtree.com，部署者 SQL 指定）。
 *       权限卡口在应用层。角色叠加——管理员同时拥有全部普通用户权限。</li>
 *   <li><b>文章归属权</b>：所有管理员可看全部文章（列表标作者），但只有作者本人或超级管理员能
 *       编辑/发布/下线/删除；管理员之间互不干涉。</li>
 *   <li><b>用户管理（任免）与硬节点维护仅超级管理员</b>；超管身份不可由接口变更（只在 0/1 之间任免）。</li>
 *   <li><b>publishedAt = 首次发布时间</b>：只在草稿→已发布时写入，之后保持不变。</li>
 * </ul>
 */
public interface AdminService {

    /** 管理后台入口校验：普通用户（role=0）抛 2002；管理员与超管都可进 */
    User requireAdmin(Long userId);

    /** 文章列表（§11.1）：含草稿，updatedAt 倒序，status 可选过滤 */
    PageVO<AdminArticleListItemVO> articles(Long adminId, Integer status, Integer page, Integer size);

    /** 新建文章（§11.2）：一律先存草稿；返回文章 id */
    AdminArticleIdVO create(Long adminId, SaveArticleDTO req);

    /** 文章详情（编辑表单回显）：含草稿、含 coverObjectKey；标作者与可操作标记 */
    AdminArticleDetailVO detail(Long adminId, Long articleId);

    /** 编辑文章（§11.3）：标题/正文/封面整体替换式；仅作者本人或超管 */
    void update(Long adminId, Long articleId, SaveArticleDTO req);

    /** 发布（§11.4）：草稿→已发布；publishedAt 只在首次发布写入 */
    void publish(Long adminId, Long articleId);

    /** 下线（§11.5）：已发布→草稿（保留文章行与首次发布时间） */
    void unpublish(Long adminId, Long articleId);

    /** 删除文章（§11.6）：物理删行，封面对象一并清理 */
    void delete(Long adminId, Long articleId);

    /** 查用户（§11.7）：keyword 按 id/邮箱/昵称定位；仅超管 */
    PageVO<AdminUserListItemVO> users(Long adminId, String keyword, Integer page, Integer size);

    /** 任免管理员（§11.8）：仅超管；role 只在 0/1 之间变更 */
    AdminUserRoleVO updateRole(Long adminId, Long targetId, Integer role);

    /** 硬节点列表（§11.10）：examYear 可选过滤；仅超管 */
    List<AdminMilestoneVO> milestones(Long adminId, Integer examYear);

    /** 新建硬节点（§11.11）；仅超管 */
    AdminMilestoneVO createMilestone(Long adminId, SaveMilestoneDTO req);

    /** 编辑硬节点（§11.12）：整体替换式；仅超管 */
    AdminMilestoneVO updateMilestone(Long adminId, Long milestoneId, SaveMilestoneDTO req);

    /** 删除硬节点（§11.13）；仅超管 */
    void deleteMilestone(Long adminId, Long milestoneId);
}
