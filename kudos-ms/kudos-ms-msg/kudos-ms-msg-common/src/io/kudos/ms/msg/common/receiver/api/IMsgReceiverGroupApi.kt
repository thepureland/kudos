package io.kudos.ms.msg.common.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * 消息接收者群组对外API
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgReceiverGroupApi {

    /**
     * 根据 id 获取接收者群组定义。
     */
    @GetMapping("/api/internal/msg/receiverGroup/getReceiverGroupById")
    fun getReceiverGroupById(@RequestParam id: String): MsgReceiverGroupCacheEntry?

    /**
     * 查询启用的接收者群组定义；传入类型时按类型过滤。
     */
    @GetMapping("/api/internal/msg/receiverGroup/listActiveReceiverGroups")
    fun listActiveReceiverGroups(
        @RequestParam(required = false) receiverGroupTypeDictCode: String?,
    ): List<MsgReceiverGroupCacheEntry>

}
