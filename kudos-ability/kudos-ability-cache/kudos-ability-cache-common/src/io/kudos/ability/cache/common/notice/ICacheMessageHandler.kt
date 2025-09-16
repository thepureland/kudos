package io.kudos.ability.cache.common.notice

/**
 * 缓存消息处理器
 */
interface ICacheMessageHandler {
    /**
     * 发送消息
     *
     * @param message
     */
    fun sendMessage(message: CacheMessage)

    /**
     * 接受消息
     *
     * @param message
     */
    fun receiveMessage(message: CacheMessage)
}
