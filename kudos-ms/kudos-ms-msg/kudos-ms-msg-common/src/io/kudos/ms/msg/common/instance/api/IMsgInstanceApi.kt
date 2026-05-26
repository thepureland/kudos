package io.kudos.ms.msg.common.instance.api

import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * Public API for message instances.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgInstanceApi {


    /**
     * Get a message instance by id.
     *
     * @param id instance primary key
     * @return MsgInstanceCacheEntry, or null if not found
     */
    @GetMapping("/api/internal/msg/instance/getInstanceById")
    fun getInstanceById(@RequestParam id: String): MsgInstanceCacheEntry?


}
