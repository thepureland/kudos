package io.kudos.ability.cache.common.notify

/**
 * 缓存消息处理器 SPI。
 *
 * `MixCache` 在 `LOCAL_REMOTE` 策略下做完远端写之后，会通过本接口的所有实现把
 * [CacheMessage] 广播给其他节点，让它们剔除自己的本地副本。具体的传输方式由各
 * cache-interservice 子模块实现（Redis pub/sub、Nacos config、MQ 等都行）。
 *
 * 通常应用只装一个 handler；多个共存时框架会全部调用一次（fan-out），这种"重复广播"
 * 应当避免，除非测试场景下故意装两套传输验证一致性。
 *
 * @author K
 * @since 1.0.0
 */
interface ICacheMessageHandler {
    /**
     * 发送一条本节点产生的缓存失效消息给其他节点。
     */
    fun sendMessage(message: CacheMessage)

    /**
     * 接收来自其他节点的消息——典型实现把消息委派给 `MixCache.clearLocal(...)` 之类的本地清理路径。
     */
    fun receiveMessage(message: CacheMessage)
}
