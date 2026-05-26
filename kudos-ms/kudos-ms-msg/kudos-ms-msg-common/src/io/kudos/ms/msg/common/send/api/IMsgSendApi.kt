package io.kudos.ms.msg.common.send.api

import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


/**
 * Public API for message sending.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgSendApi {

    /**
     * Publish a message: select template -> render -> persist instance/send records -> dispatch to MQ.
     *
     * Returns the id of [io.kudos.ms.msg.core.send.model.po.MsgSend]; returns null when the template is missing
     * or the request is invalid. MQ dispatch failure does not cause null to be returned -- the record is still
     * persisted with status FAILED_TO_SEND_TO_MQ, and the caller may retry based on it (see Batch 4's
     * MsgUnreceived for the retry mechanism).
     */
    @PostMapping("/api/internal/msg/send/publish")
    fun publish(@RequestBody request: MsgPublishRequest): String?

}
