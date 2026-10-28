package com.leehonne.examtimeline.message;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.entity.Article;
import com.leehonne.examtimeline.entity.Message;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.enums.ArticleStatus;
import com.leehonne.examtimeline.enums.UserRole;
import com.leehonne.examtimeline.message.dto.*;
import com.leehonne.examtimeline.repository.ArticleRepository;
import com.leehonne.examtimeline.repository.MessageRepository;
import com.leehonne.examtimeline.repository.UserRepository;
import com.leehonne.examtimeline.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

/**
 * 文章留言业务（契约 §9.4~9.6 / §11.14~11.16）——「读者来信」：
 * 会话 = (articleId, userId) 多轮对话串，用户 ↔ 文章作者管理员。
 *
 * <p>口径：
 * <ul>
 *   <li>可见性：会话双方 + 超管；管理员之间互不可见、用户之间互不可见</li>
 *   <li>回复：仅文章作者本人可回复（"只能回复发给自己的"）；超管可看全部但不可代回复</li>
 *   <li>红点按"是否读过"：用户看对话→消管理员回复的未读；管理员看会话→消用户留言的未读</li>
 *   <li>删除：管理员删自己文章下的、超管删任意；用户不可撤回</li>
 *   <li>文章下线/删除：留言保留历史（文章删除时 DB 级联）</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final ArticleRepository articleRepository;
    private final UserRepository userRepository;
    private final MinioStorageService storage;

    // ================= 权限卡口 =================

    /** 管理后台卡口（与 AdminService 同口径）：普通用户 2002 */
    public User requireAdmin(Long userId) {
        User u = userRepository.findById(userId)
                .orElseThrow(() -> BizException.unauthorized("用户不存在"));
        if (!u.getRole().isAtLeastAdmin()) {
            throw BizException.forbidden("需要管理员权限");
        }
        return u;
    }

    /** 会话操作权：文章作者本人或超管；其他管理员 2002（互不可见铁律）——用于查看/删除 */
    private void requireThreadOperator(User operator, Article article) {
        if (operator.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (!article.getAuthorId().equals(operator.getId())) {
            throw BizException.forbidden("只能查看自己文章的留言");
        }
    }

    /** 回复权：仅文章作者本人（超管可看全部但不可代回复——"只能回复发给自己的"） */
    private void requireThreadAuthor(User operator, Article article) {
        if (!article.getAuthorId().equals(operator.getId())) {
            throw BizException.forbidden("只能回复发给自己文章的留言");
        }
    }

    // ================= 用户侧（§9.4~9.6） =================

    /** §9.4 发留言：只能发给已发布文章的作者，不能给自己的文章留言 */
    @Transactional
    public MessageItemResponse send(Long userId, Long articleId, String content) {
        Article article = articleRepository.findById(articleId)
                .filter(a -> a.getStatus() == ArticleStatus.PUBLISHED)
                .orElseThrow(() -> BizException.notFound("文章不存在或未发布"));
        if (article.getAuthorId().equals(userId)) {
            throw BizException.badRequest("不能给自己的文章留言");
        }
        Message m = new Message();
        m.setArticleId(articleId);
        m.setUserId(userId);
        m.setSenderId(userId);
        m.setSenderRole(0);
        m.setContent(validateContent(content));
        m.setIsRead(false);
        messageRepository.save(m);
        return toMessageItem(m);
    }

    /** §9.5 用户看对话串：时间升序；顺手把管理员回复标记已读（消用户红点） */
    @Transactional
    public UserThreadResponse userThread(Long userId, Long articleId) {
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> BizException.notFound("文章不存在"));
        markRead(articleId, userId, 1);
        List<MessageItemResponse> items = messageRepository
                .findByArticleIdAndUserIdOrderByCreatedAtAsc(articleId, userId).stream()
                .map(this::toMessageItem)
                .toList();
        return new UserThreadResponse(articleId, article.getTitle(),
                displayName(article.getAuthorId()), items);
    }

    /** §9.6 用户未读回复（信息入口红点） */
    public UserUnreadResponse userUnread(Long userId) {
        long count = messageRepository.countByUserIdAndSenderRoleAndIsReadFalse(userId, 1);
        return new UserUnreadResponse(count, messageRepository.findUnreadReplyArticleIds(userId));
    }

    // ================= 管理侧（§11.14~11.16） =================

    /** §11.14 会话列表：管理员=自己文章的会话；超管=全部（可按管理员/文章筛）；最新会话在前 */
    public AdminThreadListResponse threads(Long adminId, Long filterAuthorId, Long filterArticleId,
                                           Integer page, Integer size) {
        User op = requireAdmin(adminId);
        List<Message> msgs;
        if (op.getRole() == UserRole.SUPER_ADMIN) {
            msgs = messageRepository.findAllByOrderByCreatedAtDesc();
        } else {
            // 互不可见：普通管理员只取自己文章下的
            msgs = messageRepository.findAllForAuthor(op.getId());
        }

        // 超管筛选（按文章作者 / 按文章）
        if (op.getRole() == UserRole.SUPER_ADMIN && (filterAuthorId != null || filterArticleId != null)) {
            Map<Long, Long> articleAuthor = articleRepository.findAllById(
                            msgs.stream().map(Message::getArticleId).distinct().toList()).stream()
                    .collect(Collectors.toMap(Article::getId, Article::getAuthorId));
            msgs = msgs.stream()
                    .filter(m -> filterArticleId == null || m.getArticleId().equals(filterArticleId))
                    .filter(m -> filterAuthorId == null || filterAuthorId.equals(articleAuthor.get(m.getArticleId())))
                    .toList();
        }

        // 按会话 (articleId, userId) 分组
        Map<String, List<Message>> grouped = new LinkedHashMap<>();
        for (Message m : msgs) {
            grouped.computeIfAbsent(m.getArticleId() + ":" + m.getUserId(), k -> new ArrayList<>()).add(m);
        }

        // 会话摘要（组内 createdAt 升序，便于取"最新一条"）
        List<AdminThreadItemResponse> items = new ArrayList<>();
        for (List<Message> thread : grouped.values()) {
            thread.sort(Comparator.comparing(Message::getCreatedAt));
            items.add(toThreadItem(thread));
        }
        items.sort(Comparator.comparing(AdminThreadItemResponse::getLastAt).reversed());

        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = (p - 1) * s;
        List<AdminThreadItemResponse> slice = from >= items.size() ? List.of()
                : items.subList(from, Math.min(from + s, items.size()));
        return new AdminThreadListResponse(slice, items.size(), p, s);
    }

    /** §11.15 会话详情：完整对话串；顺手把用户留言标记已读（消管理红点） */
    @Transactional
    public AdminThreadDetailResponse threadDetail(Long adminId, Long articleId, Long userId) {
        User op = requireAdmin(adminId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> BizException.notFound("文章不存在"));
        requireThreadOperator(op, article);
        markRead(articleId, userId, 0);
        List<MessageItemResponse> items = messageRepository
                .findByArticleIdAndUserIdOrderByCreatedAtAsc(articleId, userId).stream()
                .map(this::toMessageItem)
                .toList();
        User threadUser = userRepository.findById(userId).orElse(null);
        return new AdminThreadDetailResponse(articleId, userId, article.getAuthorId(), article.getTitle(),
                displayName(article.getAuthorId()), displayName(userId),
                threadUser == null ? null : threadUser.getEmail(),
                avatarUrl(threadUser), items);
    }

    /** §11.16 回复：只有该用户可见（写入会话串 senderRole=1）；仅文章作者可回，超管不可代回复 */
    @Transactional
    public MessageItemResponse reply(Long adminId, Long articleId, Long userId, String content) {
        User op = requireAdmin(adminId);
        Article article = articleRepository.findById(articleId)
                .orElseThrow(() -> BizException.notFound("文章不存在"));
        requireThreadAuthor(op, article);
        Message m = new Message();
        m.setArticleId(articleId);
        m.setUserId(userId);
        m.setSenderId(adminId);
        m.setSenderRole(1);
        m.setContent(validateContent(content));
        m.setIsRead(false);
        messageRepository.save(m);
        return toMessageItem(m);
    }

    /** 删除单条留言：管理员删自己文章下的、超管删任意；用户不可删（走管理端入口） */
    @Transactional
    public void delete(Long adminId, Long messageId) {
        User op = requireAdmin(adminId);
        Message m = messageRepository.findById(messageId)
                .orElseThrow(() -> BizException.notFound("留言不存在"));
        Article article = articleRepository.findById(m.getArticleId())
                .orElseThrow(() -> BizException.notFound("文章不存在"));
        requireThreadOperator(op, article);
        messageRepository.delete(m);
    }

    /** 管理端未读数（「留言管理」侧栏红点）：管理员=自己文章的未读；超管=全局未读 */
    public AdminUnreadResponse adminUnread(Long adminId) {
        User op = requireAdmin(adminId);
        long count = op.getRole() == UserRole.SUPER_ADMIN
                ? messageRepository.countBySenderRoleAndIsReadFalse(0)
                : messageRepository.countUnreadForAuthor(op.getId());
        return new AdminUnreadResponse(count);
    }

    // ================= 内部工具 =================

    private String validateContent(String content) {
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) {
            throw BizException.badRequest("留言内容不能为空");
        }
        if (c.length() > 300) {
            throw BizException.badRequest("留言最多 300 字");
        }
        return c;
    }

    /** 消某会话内对方（senderRole）发来的未读 */
    private void markRead(Long articleId, Long userId, Integer senderRole) {
        List<Message> unread = messageRepository
                .findByArticleIdAndUserIdAndSenderRoleAndIsReadFalse(articleId, userId, senderRole);
        if (!unread.isEmpty()) {
            unread.forEach(m -> m.setIsRead(true));
            messageRepository.saveAll(unread);
        }
    }

    private MessageItemResponse toMessageItem(Message m) {
        // 发送者解析：sender_id 优先；历史数据回退——用户留言取 user_id、管理员回复取文章作者
        Long sid = m.getSenderId();
        if (sid == null) {
            sid = m.getSenderRole() == 0 ? m.getUserId()
                    : articleRepository.findById(m.getArticleId()).map(Article::getAuthorId).orElse(null);
        }
        User sender = sid == null ? null : userRepository.findById(sid).orElse(null);
        return new MessageItemResponse(m.getId(), m.getSenderRole(), m.getContent(), m.getCreatedAt(),
                sender == null ? "用户" + m.getUserId() : displayName(sid),
                avatarUrl(sender));
    }

    /** 会话摘要条目（组内时间升序传入） */
    private AdminThreadItemResponse toThreadItem(List<Message> thread) {
        Message first = thread.get(0);
        Message last = thread.get(thread.size() - 1);
        long unread = thread.stream().filter(m -> m.getSenderRole() == 0 && !m.getIsRead()).count();
        User u = userRepository.findById(first.getUserId()).orElse(null);
        Article a = articleRepository.findById(first.getArticleId()).orElse(null);
        return new AdminThreadItemResponse(
                first.getArticleId(), first.getUserId(),
                a == null ? "" : a.getTitle(),
                a == null ? null : a.getAuthorId(),
                a == null ? "" : displayName(a.getAuthorId()),
                u == null ? "" : displayName(u.getId()),
                u == null ? null : u.getEmail(),
                avatarUrl(u),
                last.getContent(), last.getCreatedAt(), last.getSenderRole(),
                unread, thread.size());
    }

    /** 显示名：昵称优先，回退邮箱 @ 前缀（与文章作者口径一致） */
    private String displayName(Long userId) {
        return userRepository.findById(userId)
                .map(u -> u.getNickname() != null && !u.getNickname().isBlank()
                        ? u.getNickname()
                        : Optional.ofNullable(u.getEmail()).map(e -> e.split("@")[0]).orElse("用户" + userId))
                .orElse("用户" + userId);
    }

    /** 头像地址（与 ProfileService 同口径）：空 → 默认图；/ 或 http 开头（静态资源）原样；否则对象键 → 预签名 */
    private String avatarUrl(User u) {
        if (u == null) {
            return "/avatar-default.png";
        }
        String key = u.getAvatarObjectKey();
        if (key == null || key.isBlank()) {
            return "/avatar-default.png";
        }
        if (key.startsWith("/") || key.startsWith("http")) {
            return key;
        }
        return storage.signedUrl(key);
    }
}
