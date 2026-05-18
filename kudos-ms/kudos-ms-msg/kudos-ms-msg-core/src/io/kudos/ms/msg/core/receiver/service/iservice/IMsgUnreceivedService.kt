package io.kudos.ms.msg.core.receiver.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum
import io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived


/**
 * 未送达消息业务接口
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgUnreceivedService : IBaseCrudService<String, MsgUnreceived> {

    /**
     * 批量登记一次发送中失败的接收人，状态 resolved=false。
     * 调用方一般是渠道 listener 在 callback 里调；同一 (sendId, receiverId) 重复登记会再写一行
     * （便于按重试批次审计），如需去重应由调用方先查 [findUnresolvedBySend]。
     */
    fun recordFailures(
        sendId: String,
        receiverIds: Collection<String>,
        publishMethodDictCode: String,
        reason: MsgUnreceivedReasonEnum,
        tenantId: String,
    ): Int

    /**
     * 查询某次发送批次下未处理 (resolved=false) 的失败记录。
     */
    fun findUnresolvedBySend(sendId: String): List<MsgUnreceived>

    /**
     * 标记一条记录为已处理（重试成功 / admin 关闭）。
     */
    fun resolve(id: String): Boolean

    /**
     * 累加重试次数 + 记录重试时间。
     * 不改 resolved；重试成功的最终调用方应再调 [resolve]。
     */
    fun bumpRetry(id: String): Boolean

}
