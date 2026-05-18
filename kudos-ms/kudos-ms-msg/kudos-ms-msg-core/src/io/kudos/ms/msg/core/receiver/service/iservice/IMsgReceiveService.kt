package io.kudos.ms.msg.core.receiver.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive


/**
 * 消息接收业务接口
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgReceiveService : IBaseCrudService<String, MsgReceive> {


    /**
     * 拉取某用户的所有接收记录（收件箱），按创建时间倒序。
     *
     * 不分页 —— 调用方若要分页应直接走 admin 的 BaseCrudController.search；这里专供
     * "最近收件" 类小批量场景。
     *
     * @param receiverId 接收者用户 id
     * @return 接收记录列表（按 createTime DESC）
     */
    fun getReceivesByUserId(receiverId: String): List<MsgReceiveCacheEntry>

    /**
     * 统计某用户的未读接收记录数。
     * 未读 = [io.kudos.ms.msg.common.receiver.enums.MsgReceiveStatusEnum.UNREAD_CODES]
     * 之一（包含 RECEIVED + UNREAD，但不含 READ / DELETED）。
     *
     * @param receiverId 接收者用户 id
     * @return 未读数；用户从未收到过返回 0
     */
    fun getUnreadCountByUserId(receiverId: String): Int

    /**
     * 把单条接收记录标记为已读。
     * 当前状态已经是 READ / DELETED 时不修改、返回 false（避免重复触发后续审计副作用）。
     *
     * @param id 接收记录主键
     * @return true 标记成功；false 记录不存在或当前状态不允许变更
     */
    fun markRead(id: String): Boolean

    /**
     * 把某用户的所有未读接收记录批量标为已读。
     * 仅对未读状态生效，已读 / 已删除记录跳过。
     *
     * @param receiverId 接收者用户 id
     * @return 实际被更新的记录数
     */
    fun markAllReadByUserId(receiverId: String): Int


}
