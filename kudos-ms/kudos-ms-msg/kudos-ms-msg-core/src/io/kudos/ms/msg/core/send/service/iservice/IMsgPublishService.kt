package io.kudos.ms.msg.core.send.service.iservice

import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest


/**
 * Message publish orchestration service (corresponds to soul-ms-msg's NoticePublishHandler).
 *
 * Flow of a single publish call:
 *   1. Look up the template by (tenantId, eventType, msgType, locale?); returns null if not found.
 *   2. Render title/body (via [io.kudos.ms.msg.core.template.render.MsgTemplateRenderer]).
 *   3. Persist MsgInstance (message content snapshot) + MsgSend (send record, status=PENDING).
 *   4. Package the render result into [io.kudos.ms.msg.common.send.vo.MsgDispatchEvent] and
 *      use [io.kudos.ability.distributed.notify.common.api.INotifyProducer.notify] to send it
 *      to the channel's listener (notifyType = MsgPublishMethodEnum.listenerType).
 *   5. Dispatch success → MsgSend.status = SENT_TO_MQ; failure → FAILED_TO_SEND_TO_MQ.
 *
 * @author K
 * @since 1.0.0
 */
interface IMsgPublishService {

    /**
     * Publishes a message.
     *
     * @return MsgSend.id, usable for subsequent status queries; returns null when the template is not found / dispatch fails
     */
    fun publish(request: MsgPublishRequest): String?
}
