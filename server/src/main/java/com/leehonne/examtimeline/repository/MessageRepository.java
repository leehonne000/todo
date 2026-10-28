package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /** 会话串：某文章下某用户的全部留言，时间升序 */
    List<Message> findByArticleIdAndUserIdOrderByCreatedAtAsc(Long articleId, Long userId);

    /** 会话串内对方未读的留言（红点消音用） */
    List<Message> findByArticleIdAndUserIdAndSenderRoleAndIsReadFalse(Long articleId, Long userId, Integer senderRole);

    /** 用户未读回复（管理员发给我的、我还没读） */
    long countByUserIdAndSenderRoleAndIsReadFalse(Long userId, Integer senderRole);

    /** 用户有未读回复的文章列表（信息入口红点导航用） */
    @Query("select distinct m.articleId from Message m where m.userId = :userId and m.senderRole = 1 and m.isRead = false")
    List<Long> findUnreadReplyArticleIds(@Param("userId") Long userId);

    /** 管理员未读留言数：自己文章下用户发来的未读（留言管理红点） */
    @Query("select count(m) from Message m, Article a where m.articleId = a.id and a.authorId = :adminId and m.senderRole = 0 and m.isRead = false")
    long countUnreadForAuthor(@Param("adminId") Long adminId);

    /** 超管全局未读留言数（监察范围 = 全部） */
    long countBySenderRoleAndIsReadFalse(Integer senderRole);

    /** 管理员会话范围内的全部留言（服务层按会话分组；量小直接取） */
    @Query("select m from Message m, Article a where m.articleId = a.id and a.authorId = :adminId order by m.createdAt desc")
    List<Message> findAllForAuthor(@Param("adminId") Long adminId);

    /** 超管全局留言（可选按文章/用户筛后在服务层过滤） */
    List<Message> findAllByOrderByCreatedAtDesc();
}
