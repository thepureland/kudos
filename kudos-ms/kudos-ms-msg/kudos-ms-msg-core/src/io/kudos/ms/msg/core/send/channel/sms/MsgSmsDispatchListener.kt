package io.kudos.ms.msg.core.send.channel.sms

import io.kudos.ability.comm.sms.aws.handler.AwsSmsHandler
import io.kudos.ability.comm.sms.aws.model.AwsSmsRequest
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.vo.MsgDispatchEvent
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import io.kudos.ms.msg.core.send.channel.AbstractMsgChannelDispatchListener
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.util.concurrent.ConcurrentLinkedQueue
import java.util.concurrent.atomic.AtomicInteger


/**
 * SMS channel dispatch listener (AWS SNS).
 *
 * Trigger: [notifyType] = [MsgPublishMethodEnum.SMS].listenerType = `"msg.dispatch.sms"`.
 *
 * Shows the channel-extension pattern: adding SMS means subclassing
 * [AbstractMsgChannelDispatchListener] and supplying only the SMS specifics — contact way `"101"`
 * (mobile) and the per-number AWS send. Routing is automatic (the bean self-registers under its
 * notifyType), so enabling SMS requires no change to the email channel or the publisher.
 *
 * Unlike email's single batched call, [AwsSmsHandler.send] is **per phone number** and fires an
 * individual asynchronous callback each. We fan out one send per recipient and aggregate the N
 * callbacks with a countdown; once the last one returns, the per-user success/fail split is handed
 * to the base for persistence + status aggregation.
 *
 * **Only takes effect after `kudos.msg.sms.aws.access-key-id` is configured** ([ConditionalOnProperty]).
 *
 * @author K
 * @since 1.0.0
 */
@Component
@EnableConfigurationProperties(MsgSmsProperties::class)
@ConditionalOnProperty(prefix = "kudos.msg.sms.aws", name = ["access-key-id"])
open class MsgSmsDispatchListener(
    private val smsProperties: MsgSmsProperties,
    private val awsSmsHandler: AwsSmsHandler,
    userContactWayService: IUserContactWayService,
    msgSendService: IMsgSendService,
    msgReceiveService: IMsgReceiveService,
    msgUnreceivedService: IMsgUnreceivedService,
) : AbstractMsgChannelDispatchListener(
    userContactWayService, msgSendService, msgReceiveService, msgUnreceivedService,
) {

    override val publishMethod: MsgPublishMethodEnum = MsgPublishMethodEnum.SMS

    override val contactWayDictCode: String = CONTACT_WAY_MOBILE

    override fun doDispatch(
        event: MsgDispatchEvent,
        contactByUserId: Map<String, String>,
        onComplete: (successUserIds: Collection<String>, failUserIds: Collection<String>) -> Unit,
    ) {
        val entries = contactByUserId.entries.toList()
        // contactByUserId is guaranteed non-empty by the base, so remaining starts >= 1 and the
        // last callback (regardless of dispatch/callback interleaving) triggers onComplete exactly once.
        val remaining = AtomicInteger(entries.size)
        val successUserIds = ConcurrentLinkedQueue<String>()
        val failUserIds = ConcurrentLinkedQueue<String>()

        entries.forEach { (userId, phone) ->
            val req = buildRequest(phone, event)
            awsSmsHandler.send(req) { cb ->
                if (cb.statusCode in 200..299) successUserIds.add(userId) else failUserIds.add(userId)
                if (remaining.decrementAndGet() == 0) {
                    onComplete(successUserIds.toList(), failUserIds.toList())
                }
            }
        }
    }

    /**
     * Assembles the rendered body + AWS credentials into an [AwsSmsRequest] for a single recipient.
     * SMS carries body only (no subject); the rendered title is dropped.
     */
    private fun buildRequest(phone: String, event: MsgDispatchEvent): AwsSmsRequest =
        AwsSmsRequest().apply {
            region = smsProperties.region
            accessKeyId = smsProperties.accessKeyId
            accessKeySecret = smsProperties.accessKeySecret
            phoneNumber = phone
            message = event.renderedContent
        }

    companion object {
        /** The itemCode for mobile phone in the `contact_way` dictionary (see V1.0.0.2__insert_sys_dict_item.sql) */
        private const val CONTACT_WAY_MOBILE = "101"
    }
}
