package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.entity.Article;
import com.leehonne.examtimeline.entity.Message;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.enums.ArticleStatus;
import com.leehonne.examtimeline.enums.UserRole;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.ArticleMapper;
import com.leehonne.examtimeline.mapper.MessageMapper;
import com.leehonne.examtimeline.mapper.UserMapper;
import com.leehonne.examtimeline.service.MessageService;
import com.leehonne.examtimeline.service.StorageService;
import com.leehonne.examtimeline.vo.AdminThreadDetailVO;
import com.leehonne.examtimeline.vo.AdminThreadItemVO;
import com.leehonne.examtimeline.vo.AdminUnreadVO;
import com.leehonne.examtimeline.vo.MessageItemVO;
import com.leehonne.examtimeline.vo.UserThreadVO;
import com.leehonne.examtimeline.vo.UserUnreadVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 文章留言业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper，跨表统计走注解 SQL）。
 */
@Service
@RequiredArgsConstructor
public class MessageServiceImpl implements MessageService {

    private final MessageMapper messageMapper;
    private final ArticleMapper articleMapper;
    private final UserMapper userMapper;
    private final StorageService storage;

    // ================= 权限卡口 =================

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

    /** 会话操作权：文章作者本人或超管；其他管理员 2002（互不可见铁律）——用于查看/删除 */
    private void requireThreadOperator(User operator, Article article) {
        if (operator.getRole() == UserRole.SUPER_ADMIN) {
            return;
        }
        if (!article.getAuthorId().equals(operator.getId())) {
            throw BusinessException.forbidden("只能查看自己文章的留言");
        }
    }

    /** 回复权：仅文章作者本人（超管可看全部但不可代回复——"只能回复发给自己的"） */
    private void requireThreadAuthor(User operator, Article article) {
        if (!article.getAuthorId().equals(operator.getId())) {
            throw BusinessException.forbidden("只能回复发给自己文章的留言");
        }
    }

    // ================= 用户侧（§9.4~9.6） =================

    @Override
    @Transactional
    public MessageItemVO send(Long userId, Long articleId, String content) {
        Article article = articleMapper.selectById(articleId);
        if (article == null || article.getStatus() != ArticleStatus.PUBLISHED) {
            throw BusinessException.notFound("文章不存在或未发布");
        }
        if (article.getAuthorId().equals(userId)) {
            throw BusinessException.badRequest("不能给自己的文章留言");
        }
        Message m = new Message();
        m.setArticleId(articleId);
        m.setUserId(userId);
        m.setSenderId(userId);
        m.setSenderRole(0);
        m.setContent(validateContent(content));
        m.setIsRead(false);
        messageMapper.insert(m);
        return toMessageItem(m);
    }

    @Override
    @Transactional
    public UserThreadVO userThread(Long userId, Long articleId) {
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        markRead(articleId, userId, 1);
        List<MessageItemVO> items = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                        .eq(Message::getArticleId, articleId)
                        .eq(Message::getUserId, userId)
                        .orderByAsc(Message::getCreatedAt)).stream()
                .map(this::toMessageItem)
                .toList();
        return new UserThreadVO(articleId, article.getTitle(),
                displayName(article.getAuthorId()), items);
    }

    @Override
    public UserUnreadVO userUnread(Long userId) {
        long count = messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                .eq(Message::getUserId, userId)
                .eq(Message::getSenderRole, 1)
                .eq(Message::getIsRead, false));
        return new UserUnreadVO(count, messageMapper.findUnreadReplyArticleIds(userId));
    }

    // ================= 管理侧（§11.14~11.16） =================

    @Override
    public PageVO<AdminThreadItemVO> threads(Long adminId, Long filterAuthorId, Long filterArticleId,
                                             Integer page, Integer size) {
        User op = requireAdmin(adminId);
        List<Message> msgs;
        if (op.getRole() == UserRole.SUPER_ADMIN) {
            msgs = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                    .orderByDesc(Message::getCreatedAt));
        } else {
            // 互不可见：普通管理员只取自己文章下的
            msgs = messageMapper.findAllForAuthor(op.getId());
        }

        // 超管筛选（按文章作者 / 按文章）
        if (op.getRole() == UserRole.SUPER_ADMIN && (filterAuthorId != null || filterArticleId != null)) {
            Map<Long, Long> articleAuthor = articleMapper.selectBatchIds(
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
        List<AdminThreadItemVO> items = new ArrayList<>();
        for (List<Message> thread : grouped.values()) {
            thread.sort(Comparator.comparing(Message::getCreatedAt));
            items.add(toThreadItem(thread));
        }
        items.sort(Comparator.comparing(AdminThreadItemVO::getLastAt).reversed());

        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 20 : Math.min(size, 100);
        int from = (p - 1) * s;
        List<AdminThreadItemVO> slice = from >= items.size() ? List.of()
                : items.subList(from, Math.min(from + s, items.size()));
        return PageVO.of(p, s, items.size(), slice);
    }

    @Override
    @Transactional
    public AdminThreadDetailVO threadDetail(Long adminId, Long articleId, Long userId) {
        User op = requireAdmin(adminId);
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        requireThreadOperator(op, article);
        markRead(articleId, userId, 0);
        List<MessageItemVO> items = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                        .eq(Message::getArticleId, articleId)
                        .eq(Message::getUserId, userId)
                        .orderByAsc(Message::getCreatedAt)).stream()
                .map(this::toMessageItem)
                .toList();
        User threadUser = userMapper.selectById(userId);
        return new AdminThreadDetailVO(articleId, userId, article.getAuthorId(), article.getTitle(),
                displayName(article.getAuthorId()), displayName(userId),
                threadUser == null ? null : threadUser.getEmail(),
                avatarUrl(threadUser), items);
    }

    @Override
    @Transactional
    public MessageItemVO reply(Long adminId, Long articleId, Long userId, String content) {
        User op = requireAdmin(adminId);
        Article article = articleMapper.selectById(articleId);
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        requireThreadAuthor(op, article);
        Message m = new Message();
        m.setArticleId(articleId);
        m.setUserId(userId);
        m.setSenderId(adminId);
        m.setSenderRole(1);
        m.setContent(validateContent(content));
        m.setIsRead(false);
        messageMapper.insert(m);
        return toMessageItem(m);
    }

    @Override
    @Transactional
    public void delete(Long adminId, Long messageId) {
        User op = requireAdmin(adminId);
        Message m = messageMapper.selectById(messageId);
        if (m == null) {
            throw BusinessException.notFound("留言不存在");
        }
        Article article = articleMapper.selectById(m.getArticleId());
        if (article == null) {
            throw BusinessException.notFound("文章不存在");
        }
        requireThreadOperator(op, article);
        messageMapper.deleteById(messageId);
    }

    @Override
    public AdminUnreadVO adminUnread(Long adminId) {
        User op = requireAdmin(adminId);
        long count = op.getRole() == UserRole.SUPER_ADMIN
                ? messageMapper.selectCount(new LambdaQueryWrapper<Message>()
                        .eq(Message::getSenderRole, 0)
                        .eq(Message::getIsRead, false))
                : messageMapper.countUnreadForAuthor(op.getId());
        return new AdminUnreadVO(count);
    }

    // ================= 内部工具 =================

    private String validateContent(String content) {
        String c = content == null ? "" : content.trim();
        if (c.isEmpty()) {
            throw BusinessException.badRequest("留言内容不能为空");
        }
        if (c.length() > 300) {
            throw BusinessException.badRequest("留言最多 300 字");
        }
        return c;
    }

    /** 消某会话内对方（senderRole）发来的未读 */
    private void markRead(Long articleId, Long userId, Integer senderRole) {
        List<Message> unread = messageMapper.selectList(new LambdaQueryWrapper<Message>()
                .eq(Message::getArticleId, articleId)
                .eq(Message::getUserId, userId)
                .eq(Message::getSenderRole, senderRole)
                .eq(Message::getIsRead, false));
        if (!unread.isEmpty()) {
            messageMapper.update(null, new LambdaUpdateWrapper<Message>()
                    .in(Message::getId, unread.stream().map(Message::getId).toList())
                    .set(Message::getIsRead, true));
        }
    }

    private MessageItemVO toMessageItem(Message m) {
        // 发送者解析：sender_id 优先；历史数据回退——用户留言取 user_id、管理员回复取文章作者
        Long sid = m.getSenderId();
        if (sid == null) {
            Article a = articleMapper.selectById(m.getArticleId());
            sid = m.getSenderRole() == 0 ? m.getUserId()
                    : (a == null ? null : a.getAuthorId());
        }
        User sender = sid == null ? null : userMapper.selectById(sid);
        return new MessageItemVO(m.getId(), m.getSenderRole(), m.getContent(), m.getCreatedAt(),
                sender == null ? "用户" + m.getUserId() : displayName(sid),
                avatarUrl(sender));
    }

    /** 会话摘要条目（组内时间升序传入） */
    private AdminThreadItemVO toThreadItem(List<Message> thread) {
        Message first = thread.get(0);
        Message last = thread.get(thread.size() - 1);
        long unread = thread.stream().filter(m -> m.getSenderRole() == 0 && !m.getIsRead()).count();
        User u = userMapper.selectById(first.getUserId());
        Article a = articleMapper.selectById(first.getArticleId());
        return new AdminThreadItemVO(
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
        User u = userMapper.selectById(userId);
        if (u == null) {
            return "用户" + userId;
        }
        return u.getNickname() != null && !u.getNickname().isBlank()
                ? u.getNickname()
                : Optional.ofNullable(u.getEmail()).map(e -> e.split("@")[0]).orElse("用户" + userId);
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
