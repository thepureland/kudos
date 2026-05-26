package io.kudos.ms.msg.api.admin.controller.receiver

import io.kudos.ms.msg.common.receiver.vo.response.MsgUnreceivedRow
import io.kudos.ms.msg.core.receiver.service.iservice.IMsgUnreceivedService
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * Undelivered message admin controller.
 *
 * Intentionally does not extend BaseCrudController: this table is for ops/audit use; create/update/delete
 * are written by listeners and advanced via resolve / bumpRetry. There is no legitimate scenario for an
 * administrator to manually add an undelivered record.
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/admin/msg/unreceived")
class MsgUnreceivedAdminController(
    private val service: IMsgUnreceivedService,
) {

    /** List unresolved failed records under a given send batch. */
    @GetMapping("/listUnresolvedBySend")
    fun listUnresolvedBySend(@RequestParam sendId: String): List<MsgUnreceivedRow> {
        return service.findUnresolvedBySend(sendId).map { it.toRow() }
    }

    /** Mark a record as resolved. */
    @PostMapping("/resolve")
    fun resolve(@RequestParam id: String): Boolean = service.resolve(id)

    /** Increment retry count (the caller must publish once for the actual resend, then call this endpoint to record it). */
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
