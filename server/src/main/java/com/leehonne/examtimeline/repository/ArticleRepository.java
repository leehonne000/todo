package com.leehonne.examtimeline.repository;

import com.leehonne.examtimeline.entity.Article;
import com.leehonne.examtimeline.enums.ArticleStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ArticleRepository extends JpaRepository<Article, Long> {

    /** 信息模块文章列表（idx_article_status_published） */
    List<Article> findByStatusOrderByPublishedAtDesc(ArticleStatus status);

    /** 管理员查自己的文章（含草稿） */
    List<Article> findByAuthorIdOrderByUpdatedAtDesc(Long authorId);
}
