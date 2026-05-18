package io.kudos.ms.msg.common.receiver.api

import io.kudos.ms.msg.common.receiver.vo.MsgReceiveCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 消息接收对外API（收件箱）
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgReceiveApi {


    /**
     * 拉取某用户的接收记录列表，createTime DESC。
     */
    @GetMapping("/api/internal/msg/receive/getReceivesByUserId")
    fun getReceivesByUserId(@RequestParam receiverId: String): List<MsgReceiveCacheEntry>

    /**
     * 某用户未读消息数（包括 RECEIVED + UNREAD 两种状态）。
     */
    @GetMapping("/api/internal/msg/receive/getUnreadCountByUserId")
    fun getUnreadCountByUserId(@RequestParam receiverId: String): Int

    /**
     * 把单条接收记录标记为已读。当前状态非未读类时返回 false。
     */
    @PostMapping("/api/internal/msg/receive/markRead")
    fun markRead(@RequestParam id: String): Boolean

    /**
     * 批量把某用户的未读记录全部标为已读。
     *
     * @return 被更新的记录数
     */
    @PostMapping("/api/internal/msg/receive/markAllReadByUserId")
    fun markAllReadByUserId(@RequestParam receiverId: String): Int


}
