package io.kudos.ability.cache.interservice.aop

import io.kudos.ability.cache.interservice.common.ClientCacheItem
import org.springframework.util.ConcurrentReferenceHashMap

/**
 * provider 端响应 UID 生成器。
 *
 * 默认每次按响应对象 JSON 重新计算 UID。热点接口如果反复返回同一个不可变对象实例，可打开
 * uid cache 复用计算结果；缓存使用弱引用 key，避免长期持有响应对象。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class ClientCacheUidGenerator(
    private val cacheEnabled: Boolean = false
) {

    private val uidCache = ConcurrentReferenceHashMap<Any, String>(
        256,
        ConcurrentReferenceHashMap.ReferenceType.WEAK
    )

    fun generate(result: Any): String {
        if (!cacheEnabled) {
            return ClientCacheItem.genUid(result)
        }
        return uidCache.computeIfAbsent(result) { ClientCacheItem.genUid(it) }
            ?: ClientCacheItem.genUid(result)
    }
}
