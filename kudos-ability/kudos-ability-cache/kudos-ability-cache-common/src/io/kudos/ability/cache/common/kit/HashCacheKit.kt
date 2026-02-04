package io.kudos.ability.cache.common.kit

import io.kudos.ability.cache.common.core.MixHashCacheManager
import io.kudos.ability.cache.common.support.IIdEntitiesHashCache
import io.kudos.context.kit.SpringKit

/**
 * Hash 缓存工具：按 cacheName 获取 [IIdEntitiesHashCache]（策略封装后的统一抽象）。
 *
 * 与 [CacheKit] 类似，使用时通过配置自由选择三种策略（SINGLE_LOCAL / REMOTE / LOCAL_REMOTE）。
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
     * @return 该名称对应的 IIdEntitiesHashCache，未配置或未启用时返回 null
     */
    fun getHashCache(cacheName: String): IIdEntitiesHashCache? {
        val manager = SpringKit.getBeanOrNull("mixHashCacheManager") as? MixHashCacheManager ?: return null
        return manager.getHashCache(cacheName)
    }
}
