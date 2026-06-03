package io.kudos.ms.msg.core.send.channel

import com.alibaba.fastjson2.JSON
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
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Template-method base for per-channel dispatch listeners (email / sms / ...).
 *
 * ## Why this exists
 * Channel routing is already runtime: each concrete listener registers itself into the notify
 * registry under [notifyType] = [MsgPublishMethodEnum.listenerType], and the MQ consumer dispatches
 * by type. Adding a channel therefore means adding one bean — no redeploy of the others, no central
 * `switch`/factory to edit. This base removes the *other* half of the coupling: the shared dispatch
 * skeleton (status transitions, contact lookup, no-contact bookkeeping, receive/undelivered
 * persistence, success/fail aggregation) used to live entirely inside the email listener, so a second
 * channel would have copied ~80% of it. Now a channel implements only the two channel-specific bits:
 * which contact way to resolve, and how to actually send.
 *
 * ## Fixed flow (this class)
 *   1. Decode [MsgDispatchEvent] from the notify payload (fastjson JSONObject in MQ delivery).
 *   2. Advance send status to CONSUMED_FROM_MQ.
 *   3. Empty receivers → FAILED_FINAL, stop.
 *   4. Batch-resolve the channel's contact value per receiver ([contactWayDictCode]).
 *   5. Receivers without a contact → recorded as NO_CONTACT undelivered (counted as failures).
 *   6. No contactable receiver at all → FAILED_FINAL, stop.
 *   7. Delegate the actual send to [doDispatch]; on completion, persist results and aggregate status:
 *      all success → SUCCESS, none → FAILED_FINAL, mixed → SUCCESS_PARTIAL.
 *
 * ## What a channel implements ([doDispatch])
 * Perform the send (may be asynchronous) and report the per-user outcome via the supplied
 * `onComplete(successUserIds, failUserIds)` callback. Only the *contactable* users (the keys of
 * `contactByUserId`) should be reported; NO_CONTACT receivers are already accounted for by this base.
 *
 * @author K
 * @since 1.0.0
 */
abstract class AbstractMsgChannelDispatchListener(
    protected val userContactWayService: IUserContactWayService,
    protected val msgSendService: IMsgSendService,
    protected val msgReceiveService: IMsgReceiveService,
    protected val msgUnreceivedService: IMsgUnreceivedService,
) : INotifyListener {

    protected val log = LogFactory.getLog(this::class)

    /** The channel this listener serves. Drives [notifyType], the publish-method dict code, and which contact way is resolved. */
    protected abstract val publishMethod: MsgPublishMethodEnum

    /** The `contact_way` dict itemCode this channel delivers to (e.g. email = "201", mobile = "101"). */
    protected abstract val contactWayDictCode: String

    final override fun notifyType(): String = publishMethod.listenerType

    final override fun notifyProcess(notifyMessageVo: NotifyMessageVo<out Serializable>) {
        val event = decodePayload(notifyMessageVo.messageBody) ?: return

        // Advance sendId status to CONSUMED_FROM_MQ to mark that the listener has pulled it
        msgSendService.updateSendStatus(event.sendId, MsgSendStatusEnum.CONSUMED_FROM_MQ.dictCode)

        if (event.receiverIds.isEmpty()) {
            log.warn("event {0} receiverIds is empty, marking FAILED_FINAL", event.sendId)
            msgSendService.finishSend(event.sendId, 0, 0, MsgSendStatusEnum.FAILED_FINAL.dictCode)
            return
        }

        // Batch query the channel's contact value per receiver
        val contactByUserId = userContactWayService.getActiveContactValuesByUserIds(
            event.receiverIds, contactWayDictCode,
        )
        val noContactUserIds = event.receiverIds - contactByUserId.keys
        if (noContactUserIds.isNotEmpty()) {
            log.warn(
                "event {0} has {1} receivers without a {2} contact, skipping",
                event.sendId, noContactUserIds.size, publishMethod.dictCode,
            )
            // Failure tracking: receivers without a contact are directly registered as undelivered
            msgUnreceivedService.recordFailures(
                sendId = event.sendId,
                receiverIds = noContactUserIds,
                publishMethodDictCode = publishMethod.dictCode,
                reason = MsgUnreceivedReasonEnum.NO_CONTACT,
                tenantId = event.tenantId,
            )
        }
        if (contactByUserId.isEmpty()) {
            msgSendService.finishSend(event.sendId, 0, event.receiverIds.size, MsgSendStatusEnum.FAILED_FINAL.dictCode)
            return
        }

        doDispatch(event, contactByUserId) { successUserIds, failUserIds ->
            finalizeDispatch(event, successUserIds, failUserIds, noContactUserIds.size)
        }
    }

    /**
     * Channel-specific send. Deliver to the recipients in [contactByUserId] (userId → contact value)
     * and report the outcome by invoking [onComplete] exactly once with the userIds that succeeded and
     * those that failed at the channel level. Implementations may complete asynchronously.
     *
     * @param event dispatch event (carries sendId / tenantId / rendered title+body)
     * @param contactByUserId contactable receivers only (userId → resolved contact value)
     * @param onComplete completion sink — call once with (successUserIds, failUserIds)
     */
    protected abstract fun doDispatch(
        event: MsgDispatchEvent,
        contactByUserId: Map<String, String>,
        onComplete: (successUserIds: Collection<String>, failUserIds: Collection<String>) -> Unit,
    )

    /**
     * Persists the dispatch outcome and finalizes send status.
     *
     * - Successful userIds → one [MsgReceive] row each (status RECEIVED), each wrapped in runCatching
     *   so a single insert failure doesn't drag down the batch.
     * - Channel-rejected userIds → [IMsgUnreceivedService.recordFailures] with CHANNEL_REJECT (does not
     *   overlap with the NO_CONTACT batch written upstream).
     * - Aggregate: all success → SUCCESS; none → FAILED_FINAL; mixed → SUCCESS_PARTIAL. `unreachableCount`
     *   (NO_CONTACT receivers, already persisted upstream) only participates in the failCount tally.
     */
    protected open fun finalizeDispatch(
        event: MsgDispatchEvent,
        successUserIds: Collection<String>,
        failUserIds: Collection<String>,
        unreachableCount: Int,
    ) {
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

        if (failUserIds.isNotEmpty()) {
            msgUnreceivedService.recordFailures(
                sendId = event.sendId,
                receiverIds = failUserIds,
                publishMethodDictCode = publishMethod.dictCode,
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
            "{0} dispatch finished: sendId={1}, success={2}, fail={3}, status={4}",
            publishMethod.dictCode, event.sendId, successCount, failCount, status,
        )
    }

    /**
     * NotifyMqAutoConfiguration deserializes messageBody into a JSONObject and forwards it here;
     * this method converts it back to [MsgDispatchEvent]. A direct cast would work but is fragile —
     * going through fastjson once is more robust.
     */
    protected fun decodePayload(body: Serializable?): MsgDispatchEvent? {
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
}
