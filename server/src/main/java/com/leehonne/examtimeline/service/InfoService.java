package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.vo.ArticleDetailVO;
import com.leehonne.examtimeline.vo.ArticleListItemVO;
import com.leehonne.examtimeline.vo.InfoOverviewVO;

/**
 * 信息模块业务（契约 §5.1/§9）——固定信息区（倒计时+硬节点）+ 博客内容区（只读）。
 *
 * <p>口径：
 * <ul>
 *   <li>倒计时 = 用户届数下「初试」date_from − 学习日今天（可为负）</li>
 *   <li>硬节点为全局表（t_exam_milestone），按届数过滤、sortOrder 升序</li>
 *   <li>博客只含已发布（草稿对普通用户不可见，3002）；发布倒序；封面预签名 URL 可空</li>
 * </ul>
 */
public interface InfoService {

    /** 固定信息区（§9.1） */
    InfoOverviewVO overview(Long userId);

    /** 博客列表（§9.2）：只含已发布，发布倒序 */
    PageVO<ArticleListItemVO> articles(Integer page, Integer size);

    /** 文章详情（§9.3）：草稿对普通用户不可见 */
    ArticleDetailVO detail(Long articleId);
}
