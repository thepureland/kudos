package io.kudos.ms.msg.core.send.channel.email

import com.alibaba.fastjson2.JSON
import io.kudos.ability.comm.email.handler.EmailHandler
import io.kudos.ability.comm.email.model.EmailCallBackParam
import io.kudos.ability.comm.email.model.EmailRequest
import io.kudos.ability.distributed.notify.common.api.INotifyListener
import io.kudos.ability.distributed.notify.common.model.NotifyMessageVo
import io.kudos.base.logger.LogFactory
import io.kudos.ms.msg.common.receiver.enums.MsgReceiveStatusEnum
import io.kudos.ms.msg.common.receiver.enums.MsgUnreceivedReasonEnum
import io.kudos.ms.msg.common.send.enums.MsgPublishMethodEnum
import io.kudos.ms.msg.common.send.enums.MsgSendStatusEnum
import io.kudos.ms.msg.common.send.vo.MsgDispatchEvent
import io.kudos.ms.msg.core.receiver.model.po.MsgReceive
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgReceiveService
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import io.kudos.ms.msg.core.send.service.iservice.IMsgSendService
import io.kudos.ms.user.core.contact.service.iservice.IUserContactWayService
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.stereotype.Component
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Email channel dispatch listener.
 *
 * Trigger: [NotifyMessageVo.notifyType] = [MsgPublishMethodEnum.EMAIL].listenerType = `"msg.dispatch.email"`.
 *
 * Flow:
 *   1. Deserialize [MsgDispatchEvent] (the payload is a fastjson JSONObject, because
 *      NotifyMqAutoConfiguration uniformly receives it as such).
 *   2. Batch look up email addresses by receiverIds (contact_way dict code `"201"` = email).
 *   3. Treat receivers without an email as failure count.
 *   4. EmailHandler.send is asynchronous + callback-driven:
 *      - All success → write one MsgReceive row per matched receiver, status RECEIVED; increment successCount.
 *      - Partial success → split successEmails / failEmails, write RECEIVED / failure count respectively.
 *      - All failed → failure count += number of receivers.
 *   5. Final status: all success → SUCCESS; partial failure → SUCCESS_PARTIAL; all failed → FAILED_FINAL.
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
    private val userContactWayService: IUserContactWayService,
    private val msgSendService: IMsgSendService,
    private val msgReceiveService: IMsgReceiveService,
    private val msgUnreceivedService: IMsgUnreceivedService,
) : INotifyListener {

    private val log = LogFactory.getLog(this::class)

    override fun notifyType(): String = MsgPublishMethodEnum.EMAIL.listenerType

    override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
        val event = decodePayload(notifyMessageVo.messageBody) ?: return

        // Advance sendId status to CONSUMED_FROM_MQ to mark that the listener has pulled it
        msgSendService.updateSendStatus(event.sendId, MsgSendStatusEnum.CONSUMED_FROM_MQ.dictCode)

        if (event.receiverIds.isEmpty()) {
            log.warn("event {0} receiverIds is empty, marking FAILED_FINAL", event.sendId)
            msgSendService.finishSend(event.sendId, 0, 0, MsgSendStatusEnum.FAILED_FINAL.dictCode)
            return
        }

        // Batch query email addresses
        val emailByUserId = userContactWayService.getActiveContactValuesByUserIds(
            event.receiverIds, CONTACT_WAY_EMAIL,
        )
        val noEmailUserIds = event.receiverIds - emailByUserId.keys
        if (noEmailUserIds.isNotEmpty()) {
            log.warn("event {0} has {1} receivers without an email contact, skipping", event.sendId, noEmailUserIds.size)
            // Failure tracking: receivers without an email are directly registered as undelivered
            msgUnreceivedService.recordFailures(
                sendId = event.sendId,
                receiverIds = noEmailUserIds,
                publishMethodDictCode = MsgPublishMethodEnum.EMAIL.dictCode,
                reason = MsgUnreceivedReasonEnum.NO_CONTACT,
                tenantId = event.tenantId,
            )
        }
        if (emailByUserId.isEmpty()) {
            msgSendService.finishSend(event.sendId, 0, event.receiverIds.size, MsgSendStatusEnum.FAILED_FINAL.dictCode)
            return
        }

        val req = buildRequest(emailByUserId.values.toSet(), event)
        val emailToUserId = emailByUserId.entries.associate { (uid, email) -> email to uid }

        emailHandler.send(req) { cb ->
            handleCallback(event, cb, emailToUserId, noEmailUserIds.size)
        }
    }

    /**
     * Email send callback processing: translates the SMTP layer's "success/fail email lists"
     * into userId-level statistics and persists them.
     *
     * Three steps:
     * 1. Successful userIds → `MsgReceive` rows (each wrapped in runCatching to avoid a single failure dragging down the batch).
     * 2. Failed userIds → `MsgUnreceived.recordFailures` (does not overlap with the NO_CONTACT batch written in `notifyProcess`).
     * 3. Aggregate status: all failed → FAILED_FINAL; all success → SUCCESS; mixed → SUCCESS_PARTIAL.
     *
     * `unreachableCount` is included in failCount but is **not** written back to MsgUnreceived
     * (already written upstream in notifyProcess).
     *
     * @param event dispatch event (contains sendId / tenantId / template render result)
     * @param cb SMTP-layer callback (success/fail email lists)
     * @param emailToUserId reverse map of email → userId (for mapping SMTP-email-level success/failure back to business users)
     * @param unreachableCount number of NO_CONTACT receivers (already written to MsgUnreceived upstream, only participates in the failCount tally)
     * @author K
     * @since 1.0.0
     */
    private fun handleCallback(
        event: MsgDispatchEvent,
        cb: EmailCallBackParam,
        emailToUserId: Map<String, String>,
        unreachableCount: Int,
    ) {
        val successEmails = cb.successEmails.orEmpty()
        val failEmails = cb.failEmails.orEmpty()
        val successUserIds = successEmails.mapNotNull { emailToUserId[it] }
        val failUserIds = failEmails.mapNotNull { emailToUserId[it] }

        // Write receive records: only insert for successes; failures are not written to MsgReceive
        // but reflected via failCount. Batch 4's MsgUnreceived takes over persistence of "failed receivers".
        val now = LocalDateTime.now()
        successUserIds.forEach { userId ->
            val r = MsgReceive().apply {
                receiverId = userId
                sendId = event.sendId
                receiveStatusDictCode = MsgReceiveStatusEnum.RECEIVED.dictCode
                createTime = now
                updateTime = null
                tenantId = event.tenantId
            }
            runCatching { msgReceiveService.insert(r) }
                .onFailure { log.error(it, "Failed to persist MsgReceive: userId={0}, sendId={1}", userId, event.sendId) }
        }

        // Receivers rejected by SMTP are also registered as undelivered (does not overlap with the NO_CONTACT batch — that batch was already written in notifyProcess)
        if (failUserIds.isNotEmpty()) {
            msgUnreceivedService.recordFailures(
                sendId = event.sendId,
                receiverIds = failUserIds,
                publishMethodDictCode = MsgPublishMethodEnum.EMAIL.dictCode,
                reason = MsgUnreceivedReasonEnum.CHANNEL_REJECT,
                tenantId = event.tenantId,
            )
        }

        val successCount = successUserIds.size
        val failCount = failUserIds.size + unreachableCount
        val status = when {
            failCount == 0 && successCount > 0 -> MsgSendStatusEnum.SUCCESS
            successCount == 0 -> MsgSendStatusEnum.FAILED_FINAL
            else -> MsgSendStatusEnum.SUCCESS_PARTIAL
        }
        msgSendService.finishSend(event.sendId, successCount, failCount, status.dictCode)
        log.info(
            "Email dispatch finished: sendId={0}, success={1}, fail={2}, status={3}",
            event.sendId, successCount, failCount, status,
        )
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

    /**
     * NotifyMqAutoConfiguration deserializes messageBody into a JSONObject and forwards it here;
     * this method converts it back to [MsgDispatchEvent]. A direct cast would work but is fragile —
     * going through fastjson once is more robust.
     */
    private fun decodePayload(body: Serializable?): MsgDispatchEvent? {
        if (body == null) {
            log.warn("notify body is empty")
            return null
        }
        return runCatching {
            when (body) {
                is MsgDispatchEvent -> body
                else -> JSON.parseObject(JSON.toJSONString(body), MsgDispatchEvent::class.java)
            }
        }.getOrElse {
            log.error(it, "MsgDispatchEvent deserialization failed, payload type: ${body.javaClass.name}")
            null
        }
    }

    companion object {
        /** The itemCode for email in the `contact_way` dictionary (see V1.0.0.2__insert_sys_dict_item.sql) */
        private const val CONTACT_WAY_EMAIL = "201"
    }
}
