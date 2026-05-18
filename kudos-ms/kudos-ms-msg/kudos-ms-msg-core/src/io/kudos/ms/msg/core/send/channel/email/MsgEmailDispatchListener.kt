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
 * Email 渠道 dispatch listener。
 *
 * 触发：[NotifyMessageVo.notifyType] = [MsgPublishMethodEnum.EMAIL].listenerType = `"msg.dispatch.email"`。
 *
 * 流程：
 *   1. 反序列化 [MsgDispatchEvent]（payload 是 fastjson 的 JSONObject，因为 NotifyMqAutoConfiguration
 *      统一这么收的）。
 *   2. 按 receiverIds 批量查邮箱 (contact_way dict code `"201"` = email)。
 *   3. 没邮箱的当成失败计数。
 *   4. EmailHandler.send 走异步 + 回调：
 *      - 成功 → 给所有匹配上邮箱的 receiver 各写一条 MsgReceive，状态 RECEIVED；累加 successCount。
 *      - 部分成功 → 区分 successEmails / failEmails，分别写 RECEIVED / 失败计数。
 *      - 全失败 → 失败计数 += 接收者数。
 *   5. 最终 status：全部成功 → SUCCESS；部分失败 → SUCCESS_PARTIAL；全失败 → FAILED_FINAL。
 *
 * **只在 `kudos.msg.email.server-host` 配置后才生效**（[ConditionalOnProperty]）。否则不
 * 注册 bean，发到 email topic 的事件会被 [io.kudos.ability.distributed.notify.mq.init.NotifyMqAutoConfiguration]
 * 记录为 "无 listener 配置"。
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

        // 把 sendId 状态推进到 CONSUMED_FROM_MQ，标记 listener 已拉到
        msgSendService.updateSendStatus(event.sendId, MsgSendStatusEnum.CONSUMED_FROM_MQ.dictCode)

        if (event.receiverIds.isEmpty()) {
            log.warn("event {0} receiverIds 为空，标记 FAILED_FINAL", event.sendId)
            msgSendService.finishSend(event.sendId, 0, 0, MsgSendStatusEnum.FAILED_FINAL.dictCode)
            return
        }

        // 批量查邮箱
        val emailByUserId = userContactWayService.getActiveContactValuesByUserIds(
            event.receiverIds, CONTACT_WAY_EMAIL,
        )
        val noEmailUserIds = event.receiverIds - emailByUserId.keys
        if (noEmailUserIds.isNotEmpty()) {
            log.warn("event {0} 有 {1} 个接收者无 email 联系方式，跳过", event.sendId, noEmailUserIds.size)
            // 失败追踪：没邮箱的接收者直接登记成未送达
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

        // 写收件人记录：仅对成功的写入；失败的不写 MsgReceive，但通过 failCount 体现，
        // Batch 4 的 MsgUnreceived 会接管"失败接收者"的持久化。
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
                .onFailure { log.error(it, "落库 MsgReceive 失败：userId={0}, sendId={1}", userId, event.sendId) }
        }

        // SMTP 拒收的接收者也登记成未送达（不与 NO_CONTACT 那批重复 —— 那批已经在 notifyProcess 写过）
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
            "Email dispatch 完成: sendId={0}, success={1}, fail={2}, status={3}",
            event.sendId, successCount, failCount, status,
        )
    }

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
     * NotifyMqAutoConfiguration 把 messageBody 反序列化成 JSONObject 后转传过来，
     * 这里再转回 [MsgDispatchEvent]。直接 cast 也行但脆 —— 走 fastjson 一次更稳。
     */
    private fun decodePayload(body: Serializable?): MsgDispatchEvent? {
        if (body == null) {
            log.warn("notify body 为空")
            return null
        }
        return runCatching {
            when (body) {
                is MsgDispatchEvent -> body
                else -> JSON.parseObject(JSON.toJSONString(body), MsgDispatchEvent::class.java)
            }
        }.getOrElse {
            log.error(it, "MsgDispatchEvent 反序列化失败，payload type: ${body.javaClass.name}")
            null
        }
    }

    companion object {
        /** `contact_way` 字典中 email 对应的 itemCode（见 V1.0.0.2__insert_sys_dict_item.sql） */
        private const val CONTACT_WAY_EMAIL = "201"
    }
}
