package com.leehonne.examtimeline.message;

import com.leehonne.examtimeline.common.ApiResponse;
import com.leehonne.examtimeline.common.SecurityUtils;
import com.leehonne.examtimeline.message.dto.*;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

/**
 * 文章留言接口（契约 §9.4~9.6 用户侧 / §11.14~11.17 管理侧）——「读者来信」：
 * 用户就文章给作者管理员私密留言，管理员回复仅该用户可见；超管全局可见。
 */
@RestController
@RequiredArgsConstructor
public class MessageController {

    private final MessageService messageService;

    // ================= 用户侧（登录即可） =================

    /** §9.4 给文章留言（多轮会话追加一条；不能给自己的文章留言 1001） */
    @PostMapping("/api/articles/{id}/messages")
    public ApiResponse<MessageItemResponse> send(@PathVariable("id") Long articleId,
                                                 @RequestBody SendMessageRequest req) {
        return ApiResponse.ok(messageService.send(SecurityUtils.currentUserId(), articleId, req.getContent()));
    }

    /** §9.5 我与该文章作者的对话串（时间升序；打开即把回复标已读） */
    @GetMapping("/api/articles/{id}/messages")
    public ApiResponse<UserThreadResponse> thread(@PathVariable("id") Long articleId) {
        return ApiResponse.ok(messageService.userThread(SecurityUtils.currentUserId(), articleId));
    }

    /** §9.6 未读回复（信息入口红点） */
    @GetMapping("/api/messages/unread")
    public ApiResponse<UserUnreadResponse> userUnread() {
        return ApiResponse.ok(messageService.userUnread(SecurityUtils.currentUserId()));
    }

    // ================= 管理侧（role≥1） =================

    /** §11.14 会话列表：管理员=自己文章；超管=全部（authorId/articleId 可筛） */
    @GetMapping("/api/admin/messages")
    public ApiResponse<AdminThreadListResponse> threads(
            @RequestParam(value = "authorId", required = false) Long authorId,
            @RequestParam(value = "articleId", required = false) Long articleId,
            @RequestParam(value = "page", required = false) Integer page,
            @RequestParam(value = "size", required = false) Integer size) {
        return ApiResponse.ok(messageService.threads(SecurityUtils.currentUserId(), authorId, articleId, page, size));
    }

    /** §11.15 会话详情（打开即把用户留言标已读） */
    @GetMapping("/api/admin/messages/{articleId}/{userId}")
    public ApiResponse<AdminThreadDetailResponse> threadDetail(@PathVariable Long articleId, @PathVariable Long userId) {
        return ApiResponse.ok(messageService.threadDetail(SecurityUtils.currentUserId(), articleId, userId));
    }

    /** §11.16 回复用户（仅该用户可见） */
    @PostMapping("/api/admin/messages/{articleId}/{userId}")
    public ApiResponse<MessageItemResponse> reply(@PathVariable Long articleId, @PathVariable Long userId,
                                                  @RequestBody SendMessageRequest req) {
        return ApiResponse.ok(messageService.reply(SecurityUtils.currentUserId(), articleId, userId, req.getContent()));
    }

    /** §11.17 删除留言（管理员删自己文章下的，超管删任意） */
    @DeleteMapping("/api/admin/messages/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        messageService.delete(SecurityUtils.currentUserId(), id);
        return ApiResponse.ok();
    }

    /** 「留言管理」侧栏红点：未读留言数 */
    @GetMapping("/api/admin/messages/unread")
    public ApiResponse<AdminUnreadResponse> adminUnread() {
        return ApiResponse.ok(messageService.adminUnread(SecurityUtils.currentUserId()));
    }
}
