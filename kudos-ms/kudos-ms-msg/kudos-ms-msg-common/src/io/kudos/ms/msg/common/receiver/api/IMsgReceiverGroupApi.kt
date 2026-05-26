package io.kudos.ms.msg.common.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiverGroupCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam

/**
 * External API for message receiver groups.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgReceiverGroupApi {

    /**
     * Get the receiver group definition by id.
     */
    @GetMapping("/api/internal/msg/receiverGroup/getReceiverGroupById")
    fun getReceiverGroupById(@RequestParam id: String): MsgReceiverGroupCacheEntry?

    /**
     * Query active receiver group definitions; filter by type when provided.
     */
    @GetMapping("/api/internal/msg/receiverGroup/listActiveReceiverGroups")
    fun listActiveReceiverGroups(
        @RequestParam(required = false) receiverGroupTypeDictCode: String?,
    ): List<MsgReceiverGroupCacheEntry>

}
