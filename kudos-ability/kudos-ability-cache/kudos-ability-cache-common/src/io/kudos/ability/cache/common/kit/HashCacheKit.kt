package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.context.kit.SpringKit

/**
 * Hash 缓存工具：按 cacheName 获取 [IHashCache]（策略封装后的统一抽象）。
 *
 * 与 [CacheKit] 类似，使用时通过配置自由选择三种策略（SINGLE_LOCAL / REMOTE / LOCAL_REMOTE）。
 * 若 [cacheName] 未在配置中注册，将抛出 [IllegalStateException]。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object HashCacheKit {

    /**
     * 根据名称获取 Hash 缓存（带 id 对象集合）。
     *
     * @param cacheName 缓存名称（逻辑名，会按版本前缀解析）
     * @return 该名称对应的 IIdEntitiesHashCache
     * @throws IllegalStateException 当 MixHashCacheManager 不可用或该 cacheName 未配置（需在配置中增加名为 cacheName 的项）时
     */
    fun getHashCache(cacheName: String): IHashCache {
        val manager = SpringKit.getBeanOrNull("mixHashCacheManager") as? MixHashCacheManager
            ?: throw IllegalStateException("Hash 缓存管理器不可用，无法获取 Hash 缓存: $cacheName")
        return manager.getHashCache(cacheName)
            ?: throw IllegalStateException("Hash 缓存未配置: 请在缓存配置表sys_cache中增加名为 [$cacheName]的配置项")
    }

    /**
     * 判断指定 Hash 缓存是否启用本地缓存（即同一 key 再次获取可保证返回同一对象引用）。
     * 仅当策略为 [CacheStrategy.LOCAL_REMOTE] 或 [CacheStrategy.SINGLE_LOCAL] 时为 true；
     * [CacheStrategy.REMOTE] 下每次从远程反序列化为新实例，返回 false。
     *
     * @param cacheName 缓存名称（逻辑名，与 [getHashCache] 一致）
     * @return 若未配置或非 LOCAL_REMOTE/SINGLE_LOCAL 则 false
     */
    fun isLocalCacheEnabled(cacheName: String): Boolean {
        val configProvider = SpringKit.getBeanOrNull(ICacheConfigProvider::class) ?: return false
        val config = configProvider.getHashCacheConfigs()[cacheName] ?: return false
        val s = config.strategy ?: config.strategyDictCode ?: return false
        val strategy = try {
            CacheStrategy.valueOf(s)
        } catch (_: Exception) {
            return false
        }
        return strategy == CacheStrategy.LOCAL_REMOTE || strategy == CacheStrategy.SINGLE_LOCAL
    }
}
