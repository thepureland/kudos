package io.kudos.ability.cache.common.support

import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * 缓存清理监听器注册表（进程级）。
 *
 * 历史问题：旧实现 [register] 加了 `@Synchronized` 但 [getCleanListener] 没加；
 * 底层是 `mutableMapOf` / `ArrayList`，所以读侧可能拿到正在 grow 的 list 或脏值。
 * 改用 [ConcurrentHashMap] + [CopyOnWriteArrayList]：
 * - 写侧不再需要粗粒度同步；
 * - 读侧返回的快照对结构性修改"看不见"，遍历安全；
 * - 读多写极少（typical：启动期注册一次，后续只读）的场景下，COW 的写代价可以接受。
 */
object CacheCleanRegister {

    private val registerMap = ConcurrentHashMap<String, CopyOnWriteArrayList<ICacheCleanListener>>()

    fun register(cacheKey: String, cleanListener: ICacheCleanListener) {
        registerMap.computeIfAbsent(cacheKey) { CopyOnWriteArrayList() }.add(cleanListener)
    }

    fun getCleanListener(cacheKey: String): List<ICacheCleanListener>? = registerMap[cacheKey]
}
