package io.kudos.ms.msg.common.instance.api

import io.kudos.ms.msg.common.instance.vo.MsgInstanceCacheEntry
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestParam


/**
 * 消息实例对外API
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface IMsgInstanceApi {


    /**
     * 根据id获取消息实例。
     *
     * @param id 实例主键
     * @return MsgInstanceCacheEntry，找不到返回 null
     */
    @GetMapping("/api/internal/msg/instance/getInstanceById")
    fun getInstanceById(@RequestParam id: String): MsgInstanceCacheEntry?


}
