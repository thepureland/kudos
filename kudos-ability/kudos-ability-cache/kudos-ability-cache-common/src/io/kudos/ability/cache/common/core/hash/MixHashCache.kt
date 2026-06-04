package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.context.kit.SpringKit
import kotlin.reflect.KClass

/**
 * A unified view that wraps the local/remote hash caches by strategy and implements [IHashCache].
 * Reads delegate to local/remote per strategy; on writes, REMOTE writes only the remote layer, while
 * LOCAL_REMOTE writes remote first, then synchronously writes local (so the next read on this node
 * hits the latest value), and finally publishes a notification so other nodes invalidate their local copies.
 *
 * @param cacheName logical cache name (without version prefix)
 * @param strategy strategy
 * @param local local implementation, may be null
 * @param remote remote implementation, may be null
 * @param nodeId current node id (used when sending notifications, consistent with RedisCacheMessageHandler)
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
internal class MixHashCache(
    private val cacheName: String,
    private val strategy: CacheStrategy,
    private val local: IHashCache?,
    private val remote: IHashCache?,
    private val nodeId: String
) : IHashCache {

    /**
     * 取一个可用的底层 cache：远端优先（数据更全），无远端时退本地。
     * 两者都为空时配置有误，直接抛异常便于早暴露。
     *
     * @return 可用的 [IHashCache]
     * @throws IllegalStateException local + remote 都为 null 时
     * @author K
     * @since 1.0.0
     */
    private fun remoteOrLocal(): IHashCache =
        remote ?: local ?: throw IllegalStateException("Hash cache '$cacheName' has no local or remote impl")

    private val name: String = cacheName

    /**
     * Records the filterable/sortable secondary-property sets passed into the most recent write,
     * so that the read path can rebuild secondary indexes when backfilling local from remote.
     * Without this, backfilling local from remote would only write the primary data with empty
     * secondary-property indexes, and subsequent secondary-property queries would always miss locally.
     */
    @Volatile private var indexedFilterable: Set<String> = emptySet()
    @Volatile private var indexedSortable: Set<String> = emptySet()

    private fun captureIndexProps(filterable: Set<String>, sortable: Set<String>) {
        // Update only if non-empty ("replace if non-empty"): avoids wiping the record when a write
        // operation accidentally passes an empty set.
        if (filterable.isNotEmpty()) indexedFilterable = filterable
        if (sortable.isNotEmpty()) indexedSortable = sortable
    }

    /**
     * 读路径模板：本地缓存存在就走 [block]，否则返回 null（让调用方继续走远端）。
     * inline 不必（block 调用频率低，且 block 内可能涉及锁，避免内联引起问题）。
     *
     * @param T block 返回类型
     * @param block 在本地缓存上执行的读操作
     * @return block 返回值；本地缺失时 null
     * @author K
     * @since 1.0.0
     */
    private fun <T> readFromLocalFirst(block: (IHashCache) -> T): T? {
        if (local == null) return null
        return block(local)
    }

    /**
     * 发本地缓存失效通知到集群其他节点：
     * 1. 找出所有 [ICacheMessageHandler]（典型为基于 redis pubsub 的 NotifyHandler）
     * 2. 携带 cacheType=hash + nodeId（自身节点）发送
     *
     * 自身 nodeId 让接收方过滤掉自身发的消息，避免自我清空死循环。
     *
     * @param key 失效的 key；null 表示整区失效
     * @author K
     * @since 1.0.0
     */
    private fun pushHashNotify(key: Any?) {
        val handlers = SpringKit.getBeansOfType<ICacheMessageHandler>()
        if (handlers.isEmpty()) return
        val msg = CacheMessage(name, key).apply {
            cacheType = "hash"
            nodeId = this@MixHashCache.nodeId
        }
        handlers.values.forEach { it.sendMessage(msg) }
    }

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            readFromLocalFirst { it.getById(name, id, entityClass) }?.let { return it }
            val fromRemote = remote?.getById(name, id, entityClass) ?: return null
            saveOneLocal(fromRemote)
            return fromRemote
        }
        return remoteOrLocal().getById(name, id, entityClass)
    }

    /** Backfills one entry into local, rebuilding indexes from the recorded secondary-property sets (keeping parity with remote). */
    @Suppress("UNCHECKED_CAST")
    private fun saveOneLocal(entity: IIdEntity<*>) {
        local?.save(
            name,
            entity as IIdEntity<Any?>,
            indexedFilterable,
            indexedSortable
        )
    }

    /** Backfills multiple entries into local. Prefers saveBatch to avoid the overhead of N single-entry calls. */
    @Suppress("UNCHECKED_CAST")
    private fun saveManyLocal(entities: List<IIdEntity<*>>) {
        if (local == null || entities.isEmpty()) return
        local.saveBatch(
            name,
            entities as List<IIdEntity<Any?>>,
            indexedFilterable,
            indexedSortable
        )
    }

    override fun existsById(cacheName: String, id: Any): Boolean {
        return when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local?.existsById(name, id) == true
            CacheStrategy.REMOTE -> remote?.existsById(name, id) == true
            CacheStrategy.LOCAL_REMOTE -> local?.existsById(name, id) == true || remote?.existsById(name, id) == true
        }
    }

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        captureIndexProps(filterableProperties, sortableProperties)
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireNotNull(local) { "local hash cache is null" }.save(name, entity, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> requireNotNull(remote) { "remote hash cache is null" }.save(name, entity, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                requireNotNull(remote) { "remote hash cache is null" }.save(name, entity, filterableProperties, sortableProperties)
                local?.save(name, entity, filterableProperties, sortableProperties)
                pushHashNotify(entity.id)
            }
        }
    }

    override fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        captureIndexProps(filterableProperties, sortableProperties)
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireNotNull(local) { "local hash cache is null" }.saveBatch(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> requireNotNull(remote) { "remote hash cache is null" }.saveBatch(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                requireNotNull(remote) { "remote hash cache is null" }.saveBatch(name, entities, filterableProperties, sortableProperties)
                local?.saveBatch(name, entities, filterableProperties, sortableProperties)
                // The old implementation did entities.forEach { pushHashNotify(it.id) }: a storm of N publishes.
                // Switched to a single message carrying the id list; the receiver in RedisCacheMessageHandler
                // expands the Collection and calls evictLocal for each id.
                val ids = entities.mapNotNull { it.id as Any? }
                if (ids.isNotEmpty()) pushHashNotify(ids)
            }
        }
    }

    override fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        captureIndexProps(filterableProperties, sortableProperties)
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireNotNull(local) { "local hash cache is null" }.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> requireNotNull(remote) { "remote hash cache is null" }.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                requireNotNull(remote) { "remote hash cache is null" }.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
                local?.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
                pushHashNotify(id)
            }
        }
    }

    override fun <PK, E : IIdEntity<PK>> deleteByIds(
        cacheName: String,
        ids: Collection<PK>,
        entityClass: KClass<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        if (ids.isEmpty()) return
        captureIndexProps(filterableProperties, sortableProperties)
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireNotNull(local) { "local hash cache is null" }.deleteByIds(name, ids, entityClass, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> requireNotNull(remote) { "remote hash cache is null" }.deleteByIds(name, ids, entityClass, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                requireNotNull(remote) { "remote hash cache is null" }.deleteByIds(name, ids, entityClass, filterableProperties, sortableProperties)
                local?.deleteByIds(name, ids, entityClass, filterableProperties, sortableProperties)
                // Same as saveBatch: a single notification carrying the id list, avoiding an N-message Pub/Sub storm.
                pushHashNotify(ids.mapNotNull { it as Any? })
            }
        }
    }

    override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.findByIds(name, ids, entityClass) }
            if (fromLocal != null && fromLocal.size == ids.size) return fromLocal
            val fromRemote = remote?.findByIds(name, ids, entityClass) ?: return emptyList()
            saveManyLocal(fromRemote)
            return local?.findByIds(name, ids, entityClass) ?: fromRemote
        }
        return remoteOrLocal().findByIds(name, ids, entityClass)
    }

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.listAll(name, entityClass) }
            if (!fromLocal.isNullOrEmpty()) return fromLocal
            val fromRemote = remote?.listAll(name, entityClass) ?: return emptyList()
            saveManyLocal(fromRemote)
            return local?.listAll(name, entityClass) ?: fromRemote
        }
        return remoteOrLocal().listAll(name, entityClass)
    }

    override fun <PK, E : IIdEntity<PK>> listBySetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.listBySetIndex(name, entityClass, property, value) }
            if (!fromLocal.isNullOrEmpty()) return fromLocal
            val fromRemote = remote?.listBySetIndex(name, entityClass, property, value) ?: return emptyList()
            // Ensure the queried property is in the filterable index set; otherwise the local layer can never hit on this property.
            if (property !in indexedFilterable) {
                indexedFilterable = indexedFilterable + property
            }
            saveManyLocal(fromRemote)
            return local?.listBySetIndex(name, entityClass, property, value) ?: fromRemote
        }
        return remoteOrLocal().listBySetIndex(name, entityClass, property, value)
    }

    override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean
    ): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.listPageByZSetIndex(name, entityClass, zsetIndexName, offset, limit, desc) }
            if (!fromLocal.isNullOrEmpty()) return fromLocal
            val fromRemote = remote?.listPageByZSetIndex(name, entityClass, zsetIndexName, offset, limit, desc) ?: return emptyList()
            if (zsetIndexName !in indexedSortable) {
                indexedSortable = indexedSortable + zsetIndexName
            }
            saveManyLocal(fromRemote)
            return local?.listPageByZSetIndex(name, entityClass, zsetIndexName, offset, limit, desc) ?: fromRemote
        }
        return remoteOrLocal().listPageByZSetIndex(name, entityClass, zsetIndexName, offset, limit, desc)
    }

    override fun <PK, E : IIdEntity<PK>> list(
        cacheName: String,
        entityClass: KClass<E>,
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.list(name, entityClass, criteria, pageNo, pageSize, *orders) }
            if (!fromLocal.isNullOrEmpty()) return fromLocal
            val fromRemote = remote?.list(name, entityClass, criteria, pageNo, pageSize, *orders) ?: return emptyList()
            saveManyLocal(fromRemote)
            return local?.list(name, entityClass, criteria, pageNo, pageSize, *orders) ?: fromRemote
        }
        return remoteOrLocal().list(name, entityClass, criteria, pageNo, pageSize, *orders)
    }

    override fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        captureIndexProps(filterableProperties, sortableProperties)
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireNotNull(local) { "local hash cache is null" }.refreshAll(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> requireNotNull(remote) { "remote hash cache is null" }.refreshAll(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                requireNotNull(remote) { "remote hash cache is null" }.refreshAll(name, entities, filterableProperties, sortableProperties)
                local?.refreshAll(name, entities, filterableProperties, sortableProperties)
                pushHashNotify(null)
            }
        }
    }

    override fun clear(cacheName: String) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> requireNotNull(local) { "local hash cache is null" }.clear(name)
            CacheStrategy.REMOTE -> requireNotNull(remote) { "remote hash cache is null" }.clear(name)
            CacheStrategy.LOCAL_REMOTE -> {
                requireNotNull(remote) { "remote hash cache is null" }.clear(name)
                local?.clear(name)
                pushHashNotify(null)
            }
        }
    }
}