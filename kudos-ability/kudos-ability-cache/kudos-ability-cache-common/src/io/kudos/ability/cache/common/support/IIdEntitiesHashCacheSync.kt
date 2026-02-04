package io.kudos.ability.cache.common.support

/**
 * Hash 缓存“仅清理本地”扩展接口，用于收到 Redis 通知后清理本地存储，不暴露给业务写逻辑。
 *
 * 仅本地实现（如 CaffeineIdEntitiesHashCache）需要实现；远程实现不实现。
 *
 * @author K
 * @since 1.0.0
 */
interface IIdEntitiesHashCacheSync {

    /**
     * 清空该 cacheName 下本地主数据与索引（对应远程 refreshAll）。
     */
    fun clearLocal(cacheName: String)

    /**
     * 仅从本地移除该 id（对应远程 deleteById 或 save 后失效该 id）。
     */
    fun evictLocal(cacheName: String, id: Any)
}
