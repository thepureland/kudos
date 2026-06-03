package io.kudos.ms.msg.core.send.channel.email

import io.kudos.ability.comm.email.handler.EmailHandler
import io.kudos.ability.comm.email.model.EmailRequest
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


/**
 * Email channel dispatch listener.
 *
 * Trigger: [notifyType] = [MsgPublishMethodEnum.EMAIL].listenerType = `"msg.dispatch.email"`.
 *
 * The shared dispatch skeleton (status transitions, contact lookup, no-contact / channel-reject
 * bookkeeping, success/fail aggregation) lives in [AbstractMsgChannelDispatchListener]; this class
 * only contributes the email specifics: contact way `"201"` and an async SMTP send whose callback
 * splits success/fail email lists back into userIds.
 *
 * **Only takes effect after `kudos.msg.email.server-host` is configured** ([ConditionalOnProperty]).
 * Otherwise the bean is not registered, and events sent to the email topic will be logged as
 * "no listener configured" by [io.kudos.ability.distributed.notify.mq.init.NotifyMqAutoConfiguration].
 *
 * @author K
 * @since 1.0.0
 */
@Component
@EnableConfigurationProperties(MsgEmailProperties::class)
@ConditionalOnProperty(prefix = "kudos.msg.email", name = ["server-host"])
open class MsgEmailDispatchListener(
    private val emailProperties: MsgEmailProperties,
    private val emailHandler: EmailHandler,
    userContactWayService: IUserContactWayService,
    msgSendService: IMsgSendService,
    msgReceiveService: IMsgReceiveService,
    msgUnreceivedService: IMsgUnreceivedService,
) : AbstractMsgChannelDispatchListener(
    userContactWayService, msgSendService, msgReceiveService, msgUnreceivedService,
) {

    override val publishMethod: MsgPublishMethodEnum = MsgPublishMethodEnum.EMAIL

    override val contactWayDictCode: String = CONTACT_WAY_EMAIL

    override fun doDispatch(
        event: MsgDispatchEvent,
        contactByUserId: Map<String, String>,
        onComplete: (successUserIds: Collection<String>, failUserIds: Collection<String>) -> Unit,
    ) {
        val req = buildRequest(contactByUserId.values.toSet(), event)
        val emailToUserId = contactByUserId.entries.associate { (uid, email) -> email to uid }

        // EmailHandler.send is asynchronous + callback-driven; map SMTP-level success/fail email lists
        // back to userIds and hand them to the base for persistence + status aggregation.
        emailHandler.send(req) { cb ->
            val successUserIds = cb.successEmails.orEmpty().mapNotNull { emailToUserId[it] }
            val failUserIds = cb.failEmails.orEmpty().mapNotNull { emailToUserId[it] }
            onComplete(successUserIds, failUserIds)
        }
    }

    /**
     * Assembles the template render result + SMTP server config into an [EmailRequest].
     * `receivers` is copied via toMutableSet to avoid the downstream [EmailHandler] directly holding
     * the caller's immutable collection and then attempting to modify it.
     *
     * @param toAddresses set of recipient email addresses
     * @param event dispatch event (contains rendered subject/body/tenantId)
     * @return a request object that can be passed directly to [EmailHandler.send]
     * @author K
     * @since 1.0.0
     */
    private fun buildRequest(toAddresses: Set<String>, event: MsgDispatchEvent): EmailRequest =
        EmailRequest().apply {
            subject = event.renderedTitle
            body = event.renderedContent
            receivers = toAddresses.toMutableSet()
            senderAccount = emailProperties.senderAccount
            senderPassword = emailProperties.senderPassword
            serverHost = emailProperties.serverHost
            serverPort = emailProperties.serverPort
            fromMailAddress = emailProperties.fromMailAddress
            ssl = emailProperties.ssl
            protocol = emailProperties.protocol
            smtpAuth = emailProperties.smtpAuth
            encoding = emailProperties.encoding
            htmlFormat = emailProperties.htmlFormat
            sendpartial = emailProperties.sendpartial
        }

    companion object {
        /** The itemCode for email in the `contact_way` dictionary (see V1.0.0.2__insert_sys_dict_item.sql) */
        private const val CONTACT_WAY_EMAIL = "201"
    }
}
