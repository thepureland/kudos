package io.kudos.ms.msg.api.admin.controller.receiver

import io.kudos.ms.msg.common.receiver.vo.response.MsgUnreceivedRow
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * 未送达消息管理控制器。
 *
 * 故意不继承 BaseCrudController：本表是运营/审计用，create/update/delete 由 listener 写入和
 * resolve / bumpRetry 推进，没有"管理员手工新增一条未送达"的合理场景。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/unreceived")
class MsgUnreceivedAdminController(
    private val service: IMsgUnreceivedService,
) {

    /** 列出某次发送批次下尚未处理的失败记录 */
    @GetMapping("/listUnresolvedBySend")
    fun listUnresolvedBySend(@RequestParam sendId: String): List<MsgUnreceivedRow> {
        return service.findUnresolvedBySend(sendId).map { it.toRow() }
    }

    /** 标记一条为已处理 */
    @PostMapping("/resolve")
    fun resolve(@RequestParam id: String): Boolean = service.resolve(id)

    /** 累加重试次数（实际重发动作由调用方自行 publish 一次，再来调本接口记账） */
    @PostMapping("/bumpRetry")
    fun bumpRetry(@RequestParam id: String): Boolean = service.bumpRetry(id)

    private fun io.kudos.ms.msg.core.receiver.model.po.MsgUnreceived.toRow() = MsgUnreceivedRow(
        id = id,
        receiverId = receiverId,
        sendId = sendId,
        publishMethodDictCode = publishMethodDictCode,
        failReason = failReason,
        retryCount = retryCount,
        lastRetryTime = lastRetryTime,
        resolved = resolved,
        createTime = createTime,
        updateTime = updateTime,
        tenantId = tenantId,
    )
}
