package com.leehonne.examtimeline.service;

import com.leehonne.examtimeline.common.PageVO;
import com.leehonne.examtimeline.entity.User;
import com.leehonne.examtimeline.vo.AdminThreadDetailVO;
import com.leehonne.examtimeline.vo.AdminThreadItemVO;
import com.leehonne.examtimeline.vo.AdminUnreadVO;
import com.leehonne.examtimeline.vo.MessageItemVO;
import com.leehonne.examtimeline.vo.UserThreadVO;
import com.leehonne.examtimeline.vo.UserUnreadVO;

/**
 * 文章留言业务（契约 §9.4~9.6 / §11.14~11.16）——「读者来信」：
 * 会话 = (articleId, userId) 多轮对话串，用户 ↔ 文章作者管理员。
 *
 * <p>口径：
 * <ul>
 *   <li>可见性：会话双方 + 超管；管理员之间互不可见、用户之间互不可见</li>
 *   <li>回复：仅文章作者本人可回复；超管可看全部但不可代回复</li>
 *   <li>红点按"是否读过"：用户看对话→消管理员回复的未读；管理员看会话→消用户留言的未读</li>
 *   <li>删除：管理员删自己文章下的、超管删任意；用户不可撤回</li>
 * </ul>
 */
public interface MessageService {

    /** 管理后台卡口（与 AdminService 同口径）：普通用户 2002 */
    User requireAdmin(Long userId);

    /** §9.4 发留言：只能发给已发布文章的作者，不能给自己的文章留言 */
    MessageItemVO send(Long userId, Long articleId, String content);

    /** §9.5 用户看对话串：时间升序；顺手把管理员回复标记已读（消用户红点） */
    UserThreadVO userThread(Long userId, Long articleId);

    /** §9.6 用户未读回复（信息入口红点） */
    UserUnreadVO userUnread(Long userId);

    /** §11.14 会话列表：管理员=自己文章的会话；超管=全部（可按管理员/文章筛）；最新会话在前 */
    PageVO<AdminThreadItemVO> threads(Long adminId, Long filterAuthorId, Long filterArticleId,
                                      Integer page, Integer size);

    /** §11.15 会话详情：完整对话串；顺手把用户留言标记已读（消管理红点） */
    AdminThreadDetailVO threadDetail(Long adminId, Long articleId, Long userId);

    /** §11.16 回复：只有该用户可见；仅文章作者可回，超管不可代回复 */
    MessageItemVO reply(Long adminId, Long articleId, Long userId, String content);

    /** 删除单条留言：管理员删自己文章下的、超管删任意；用户不可删 */
    void delete(Long adminId, Long messageId);

    /** 管理端未读数（「留言管理」侧栏红点）：管理员=自己文章的未读；超管=全局未读 */
    AdminUnreadVO adminUnread(Long adminId);
}
