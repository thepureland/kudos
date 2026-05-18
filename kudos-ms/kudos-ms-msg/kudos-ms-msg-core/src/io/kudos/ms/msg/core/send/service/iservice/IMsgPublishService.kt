package io.kudos.ms.msg.core.send.service.iservice

import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest


/**
 * 消息发布编排服务（对应 soul-ms-msg 的 NoticePublishHandler）。
 *
 * 一次 publish 调用的流程：
 *   1. 用 (tenantId, eventType, msgType, locale?) 找模板；找不到直接返回 null。
 *   2. 渲染标题/正文 (走 [io.kudos.ms.msg.core.template.render.MsgTemplateRenderer])。
 *   3. 落 MsgInstance（消息内容快照） + MsgSend（发送记录，status=PENDING）。
 *   4. 把渲染结果打包成 [io.kudos.ms.msg.common.send.vo.MsgDispatchEvent]，
 *      用 [io.kudos.ability.distributed.notify.common.api.INotifyProducer.notify] 投到
 *      渠道对应的 listener (notifyType = MsgPublishMethodEnum.listenerType)。
 *   5. 投递成功 → MsgSend.status = SENT_TO_MQ；失败 → FAILED_TO_SEND_TO_MQ。
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgPublishService {

    /**
     * 发布一条消息。
     *
     * @return MsgSend.id，可用于后续状态查询；模板找不到 / 投递失败时返回 null
     */
    fun publish(request: MsgPublishRequest): String?
}
