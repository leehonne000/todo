package com.leehonne.examtimeline.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.Article;
import com.leehonne.examtimeline.entity.ExamMilestone;
import com.leehonne.examtimeline.enums.ArticleStatus;
import com.leehonne.examtimeline.exception.BusinessException;
import com.leehonne.examtimeline.mapper.ArticleMapper;
import com.leehonne.examtimeline.mapper.ExamMilestoneMapper;
import com.leehonne.examtimeline.mapper.UserMapper;
import com.leehonne.examtimeline.service.InfoService;
import com.leehonne.examtimeline.service.StorageService;
import com.leehonne.examtimeline.vo.ArticleDetailVO;
import com.leehonne.examtimeline.vo.ArticleListItemVO;
import com.leehonne.examtimeline.vo.InfoOverviewVO;
import com.leehonne.examtimeline.vo.MilestoneVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 信息模块业务实现——持久层 MyBatis-Plus（BaseMapper + LambdaQueryWrapper + selectPage 分页）。
 */
@Service
@RequiredArgsConstructor
public class InfoServiceImpl implements InfoService {

    private final UserMapper userMapper;
    private final ExamMilestoneMapper milestoneMapper;
    private final ArticleMapper articleMapper;
    private final StorageService storage;

    @Override
    public InfoOverviewVO overview(Long userId) {
        var user = userMapper.selectById(userId);
        if (user == null) {
            throw BusinessException.unauthorized("用户不存在");
        }
        Integer examYear = user.getExamYear();

        List<MilestoneVO> milestones = milestoneMapper.selectList(new LambdaQueryWrapper<ExamMilestone>()
                        .eq(ExamMilestone::getExamYear, examYear)
                        .orderByAsc(ExamMilestone::getSortOrder)).stream()
                .map(this::toMilestone)
                .toList();

        ExamMilestone firstExam = milestoneMapper.selectOne(new LambdaQueryWrapper<ExamMilestone>()
                .eq(ExamMilestone::getExamYear, examYear)
                .eq(ExamMilestone::getName, "初试"));
        Integer daysLeft = firstExam == null ? null
                : (int) ChronoUnit.DAYS.between(StudyDays.today(), firstExam.getDateFrom());
        return new InfoOverviewVO(examYear, daysLeft, milestones);
    }

    @Override
    public PageVO<ArticleListItemVO> articles(Integer page, Integer size) {
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);

        IPage<Article> pageResult = articleMapper.selectPage(new Page<>(p, s),
                new LambdaQueryWrapper<Article>()
                        .eq(Article::getStatus, ArticleStatus.PUBLISHED)
                        .orderByDesc(Article::getPublishedAt));

        List<ArticleListItemVO> items = pageResult.getRecords().stream()
                .map(a -> new ArticleListItemVO(a.getId(), a.getTitle(), authorName(a.getAuthorId()),
                        coverUrl(a), a.getPublishedAt()))
                .toList();
        return PageVO.of(p, s, pageResult.getTotal(), items);
    }

    @Override
    public ArticleDetailVO detail(Long articleId) {
        Article a = articleMapper.selectById(articleId);
        if (a == null || a.getStatus() != ArticleStatus.PUBLISHED) {
            throw BusinessException.notFound("文章不存在或未发布");
        }
        return new ArticleDetailVO(a.getId(), a.getAuthorId(), a.getTitle(), authorName(a.getAuthorId()),
                a.getContent(), coverUrl(a), a.getPublishedAt());
    }

    // ---- 组装 ----

    /** 作者显示名：昵称优先，缺省回退邮箱 @ 前缀（与 §3.5 注册默认昵称口径一致） */
    private String authorName(Long authorId) {
        var u = userMapper.selectById(authorId);
        if (u == null) {
            return "未知";
        }
        return u.getNickname() != null && !u.getNickname().isBlank()
                ? u.getNickname()
                : u.getEmail().substring(0, u.getEmail().indexOf('@'));
    }

    private MilestoneVO toMilestone(ExamMilestone m) {
        return new MilestoneVO(m.getName(), m.getDateFrom(), m.getDateTo(), m.getSortOrder());
    }

    private String coverUrl(Article a) {
        return a.getCoverObjectKey() == null || a.getCoverObjectKey().isBlank()
                ? null
                : storage.signedUrl(a.getCoverObjectKey());
    }
}
