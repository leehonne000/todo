package com.leehonne.examtimeline.controller;

import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.common.Result;
import com.leehonne.examtimeline.common.context.CurrentUserContext;
import com.leehonne.examtimeline.dto.*;
import java.util.List;
import com.leehonne.examtimeline.service.*;
import com.leehonne.examtimeline.vo.*;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 管理后台接口（契约 §11）——需 role≥1（管理员/超级管理员），普通用户一律 2002。
 * 文章 CRUD/发布/下线（仅作者本人或超管可操作）+ 用户查询/任免（仅超管）+ 硬节点维护（仅超管）；
 * 登录复用 §3 邮箱+密码。
 */
@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AdminService adminService;

    // ---- 文章管理 ----

    /** §11.1 文章列表（含草稿）：status 0=草稿 1=已发布（可选）、分页 */
    @GetMapping("/articles")
    public Result<PageVO<AdminArticleListItemVO>> articles(
            @RequestParam(required = false) Integer status,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(adminService.articles(CurrentUserContext.currentUserId(), status, page, size));
    }

    /** §11.2 新建文章（存草稿）；封面先走 §5.5 上传拿临时键 */
    @PostMapping("/articles")
    public Result<AdminArticleIdVO> create(@RequestBody SaveArticleDTO req) {
        return Result.ok(adminService.create(CurrentUserContext.currentUserId(), req));
    }

    /** §11.9 文章详情（编辑回显，含草稿） */
    @GetMapping("/articles/{id}")
    public Result<AdminArticleDetailVO> detail(@PathVariable Long id) {
        return Result.ok(adminService.detail(CurrentUserContext.currentUserId(), id));
    }

    /** §11.3 编辑文章（标题/正文/封面整体替换式） */
    @PutMapping("/articles/{id}")
    public Result<Void> update(@PathVariable Long id, @RequestBody SaveArticleDTO req) {
        adminService.update(CurrentUserContext.currentUserId(), id, req);
        return Result.ok();
    }

    /** §11.4 发布：草稿→已发布（publishedAt 首次发布写入） */
    @PostMapping("/articles/{id}/publish")
    public Result<Void> publish(@PathVariable Long id) {
        adminService.publish(CurrentUserContext.currentUserId(), id);
        return Result.ok();
    }

    /** §11.5 下线：已发布→草稿（可再次发布） */
    @PostMapping("/articles/{id}/unpublish")
    public Result<Void> unpublish(@PathVariable Long id) {
        adminService.unpublish(CurrentUserContext.currentUserId(), id);
        return Result.ok();
    }

    /** §11.6 删除文章（物理删除，封面对象一并清理） */
    @DeleteMapping("/articles/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        adminService.delete(CurrentUserContext.currentUserId(), id);
        return Result.ok();
    }

    // ---- 用户管理 ----

    /** §11.7 查用户：keyword 按 id/邮箱/昵称定位（提拔入口）、分页 */
    @GetMapping("/users")
    public Result<PageVO<AdminUserListItemVO>> users(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return Result.ok(adminService.users(CurrentUserContext.currentUserId(), keyword, page, size));
    }

    /** §11.8 提拔/取消超级管理员：{role:1}=提拔 {role:0}=取消 */
    @PostMapping("/users/{id}/role")
    public Result<AdminUserRoleVO> updateRole(@PathVariable Long id, @RequestBody UpdateRoleDTO req) {
        return Result.ok(adminService.updateRole(CurrentUserContext.currentUserId(), id, req.getRole()));
    }

    // ---- 硬节点管理（§11.10~11.13 增补） ----

    /** §11.10 硬节点列表：examYear 可选过滤 */
    @GetMapping("/milestones")
    public Result<List<AdminMilestoneVO>> milestones(
            @RequestParam(required = false) Integer examYear) {
        return Result.ok(adminService.milestones(CurrentUserContext.currentUserId(), examYear));
    }

    /** §11.11 新建硬节点 */
    @PostMapping("/milestones")
    public Result<AdminMilestoneVO> createMilestone(@RequestBody SaveMilestoneDTO req) {
        return Result.ok(adminService.createMilestone(CurrentUserContext.currentUserId(), req));
    }

    /** §11.12 编辑硬节点（整体替换式） */
    @PutMapping("/milestones/{id}")
    public Result<AdminMilestoneVO> updateMilestone(@PathVariable Long id, @RequestBody SaveMilestoneDTO req) {
        return Result.ok(adminService.updateMilestone(CurrentUserContext.currentUserId(), id, req));
    }

    /** §11.13 删除硬节点 */
    @DeleteMapping("/milestones/{id}")
    public Result<Void> deleteMilestone(@PathVariable Long id) {
        adminService.deleteMilestone(CurrentUserContext.currentUserId(), id);
        return Result.ok();
    }
}
