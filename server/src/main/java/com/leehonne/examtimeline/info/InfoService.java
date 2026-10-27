package com.leehonne.examtimeline.info;

import com.leehonne.examtimeline.common.BizException;
import com.leehonne.examtimeline.common.StudyDays;
import com.leehonne.examtimeline.entity.Article;
import com.leehonne.examtimeline.entity.ExamMilestone;
import com.leehonne.examtimeline.enums.ArticleStatus;
import com.leehonne.examtimeline.info.dto.ArticleDetailResponse;
import com.leehonne.examtimeline.info.dto.ArticleListItemResponse;
import com.leehonne.examtimeline.info.dto.ArticleListResponse;
import com.leehonne.examtimeline.info.dto.InfoOverviewResponse;
import com.leehonne.examtimeline.info.dto.MilestoneResponse;
import com.leehonne.examtimeline.repository.ArticleRepository;
import com.leehonne.examtimeline.repository.ExamMilestoneRepository;
import com.leehonne.examtimeline.repository.UserRepository;
import com.leehonne.examtimeline.storage.MinioStorageService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * 信息模块业务（契约 §5.1/§9）——固定信息区（倒计时+硬节点）+ 博客内容区（只读）。
 *
 * <p>口径：
 * <ul>
 *   <li>倒计时 = 用户届数下「初试」date_from − <strong>学习日</strong>今天（可为负）</li>
 *   <li>硬节点为全局表（t_exam_milestone），按届数过滤、sortOrder 升序</li>
 *   <li>博客只含已发布（草稿对普通用户不可见，3002）；发布倒序；封面预签名 URL 可空</li>
 * </ul>
 */
@Service
@RequiredArgsConstructor
public class InfoService {

    private final UserRepository userRepository;
    private final ExamMilestoneRepository milestoneRepository;
    private final ArticleRepository articleRepository;
    private final MinioStorageService storage;

    /** 固定信息区（§9.1） */
    public InfoOverviewResponse overview(Long userId) {
        Integer examYear = userRepository.findById(userId)
                .orElseThrow(() -> BizException.unauthorized("用户不存在"))
                .getExamYear();

        List<MilestoneResponse> milestones = milestoneRepository
                .findByExamYearOrderBySortOrderAsc(examYear).stream()
                .map(this::toMilestone)
                .toList();

        Integer daysLeft = milestoneRepository.findByExamYearAndName(examYear, "初试")
                .map(m -> (int) ChronoUnit.DAYS.between(StudyDays.today(), m.getDateFrom()))
                .orElse(null);
        return new InfoOverviewResponse(examYear, daysLeft, milestones);
    }

    /** 博客列表（§9.2）：只含已发布，发布倒序 */
    public ArticleListResponse articles(Integer page, Integer size) {
        List<Article> all = articleRepository.findByStatusOrderByPublishedAtDesc(ArticleStatus.PUBLISHED);
        int p = page == null || page < 1 ? 1 : page;
        int s = size == null || size < 1 ? 10 : Math.min(size, 50);
        int from = (p - 1) * s;
        List<Article> slice = from >= all.size() ? List.of()
                : all.subList(from, Math.min(from + s, all.size()));

        List<ArticleListItemResponse> items = new ArrayList<>();
        for (Article a : slice) {
            items.add(new ArticleListItemResponse(a.getId(), a.getTitle(), coverUrl(a), a.getPublishedAt()));
        }
        return new ArticleListResponse(items, all.size(), p, s);
    }

    /** 文章详情（§9.3）：草稿对普通用户不可见 */
    public ArticleDetailResponse detail(Long articleId) {
        Article a = articleRepository.findById(articleId)
                .filter(x -> x.getStatus() == ArticleStatus.PUBLISHED)
                .orElseThrow(() -> BizException.notFound("文章不存在或未发布"));
        return new ArticleDetailResponse(a.getId(), a.getTitle(), a.getContent(), coverUrl(a), a.getPublishedAt());
    }

    // ---- 组装 ----

    private MilestoneResponse toMilestone(ExamMilestone m) {
        return new MilestoneResponse(m.getName(), m.getDateFrom(), m.getDateTo(), m.getSortOrder());
    }

    private String coverUrl(Article a) {
        return a.getCoverObjectKey() == null || a.getCoverObjectKey().isBlank()
                ? null
                : storage.signedUrl(a.getCoverObjectKey());
    }
}
