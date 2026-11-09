package com.leehonne.examtimeline.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.leehonne.examtimeline.entity.Message;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 留言 Mapper（t_message）—— 通用 CRUD 走 BaseMapper，跨表统计/联查走注解 SQL。
 */
public interface MessageMapper extends BaseMapper<Message> {

    /** 管理员视角：与自己文章相关的全部留言（join 文章表按创建时间倒序） */
    @Select("""
            SELECT m.* FROM t_message m
            JOIN t_article a ON m.article_id = a.id
            WHERE a.author_id = #{adminId}
            ORDER BY m.created_at DESC
            """)
    List<Message> findAllForAuthor(@Param("adminId") Long adminId);

    /** 管理员视角：自己文章下未读的用户留言数 */
    @Select("""
            SELECT COUNT(*) FROM t_message m
            JOIN t_article a ON m.article_id = a.id
            WHERE a.author_id = #{adminId} AND m.sender_role = 0 AND m.is_read = 0
            """)
    long countUnreadForAuthor(@Param("adminId") Long adminId);

    /** 用户视角：有未读管理员回复的文章 id 列表（信息入口红点） */
    @Select("""
            SELECT DISTINCT article_id FROM t_message
            WHERE user_id = #{userId} AND sender_role = 1 AND is_read = 0
            """)
    List<Long> findUnreadReplyArticleIds(@Param("userId") Long userId);
}
