package io.kudos.ms.msg.common.send.api

import io.kudos.ms.msg.common.send.vo.request.MsgPublishRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


/**
 * 消息发送对外API
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgSendApi {

    /**
     * 发布一条消息：选模板 → 渲染 → 落 instance/send 记录 → 投 MQ。
     *
     * 返回 [io.kudos.ms.msg.core.send.model.po.MsgSend] 的 id；模板缺失或入参不合法时返回 null。
     * 投 MQ 失败不会使方法返回 null —— 记录仍然落库，status 标为 FAILED_TO_SEND_TO_MQ，
     * 调用方可据此重试（重试机制见 Batch 4 的 MsgUnreceived）。
     */
    @PostMapping("/api/internal/msg/send/publish")
    fun publish(@RequestBody request: MsgPublishRequest): String?

}
