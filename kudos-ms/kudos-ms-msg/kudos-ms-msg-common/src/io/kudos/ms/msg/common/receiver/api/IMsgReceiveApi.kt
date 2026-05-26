package io.kudos.ms.msg.common.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * External API for message receive (inbox).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgReceiveApi {


    /**
     * Fetch the receive record list for a user, ordered by createTime DESC.
     */
    @GetMapping("/api/internal/msg/receive/getReceivesByUserId")
    fun getReceivesByUserId(@RequestParam receiverId: String): List<MsgReceiveCacheEntry>

    /**
     * Unread message count for a user (includes both RECEIVED and UNREAD statuses).
     */
    @GetMapping("/api/internal/msg/receive/getUnreadCountByUserId")
    fun getUnreadCountByUserId(@RequestParam receiverId: String): Int

    /**
     * Mark a single receive record as read. Returns false when the current status is not an unread one.
     */
    @PostMapping("/api/internal/msg/receive/markRead")
    fun markRead(@RequestParam id: String): Boolean

    /**
     * Bulk mark all unread records for a user as read.
     *
     * @return number of records updated
     */
    @PostMapping("/api/internal/msg/receive/markAllReadByUserId")
    fun markAllReadByUserId(@RequestParam receiverId: String): Int


}
