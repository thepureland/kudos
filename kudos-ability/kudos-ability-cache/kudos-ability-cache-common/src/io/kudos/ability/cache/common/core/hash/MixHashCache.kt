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
 * 按策略封装本地/远程 Hash 缓存的统一视图，实现 [IHashCache]。
 * 读按策略委托 local/remote；写时 REMOTE 只写远程，LOCAL_REMOTE 先写远程、再同步写本地（保证本节点下次读命中最新）、最后发通知让其他节点失效本地。
 *
 * @param cacheName 逻辑缓存名（未加版本前缀）
 * @param strategy 策略
 * @param local 本地实现，可为 null
 * @param remote 远程实现，可为 null
 * @param nodeId 当前节点 ID（发通知用，与 RedisCacheMessageHandler 一致）
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

    private fun remoteOrLocal(): IHashCache =
        remote ?: local ?: throw IllegalStateException("Hash cache '$cacheName' has no local or remote impl")

    private val name: String = cacheName

    /**
     * 记录最近一次写操作传入的 filterable/sortable 副属性集合，供读路径从远端回填本地时重建二级索引。
     * 如果不记录这些信息，远端回填本地永远只写主数据、副属性索引为空，后续按副属性的查询永远 miss 本地。
     */
    @Volatile private var indexedFilterable: Set<String> = emptySet()
    @Volatile private var indexedSortable: Set<String> = emptySet()

    private fun captureIndexProps(filterable: Set<String>, sortable: Set<String>) {
        // 非空才更新（"replace if non-empty"）：避免某次写操作误传空集合时把记录抹掉。
        if (filterable.isNotEmpty()) indexedFilterable = filterable
        if (sortable.isNotEmpty()) indexedSortable = sortable
    }

    private fun <T> readFromLocalFirst(block: (IHashCache) -> T): T? {
        if (local == null) return null
        return block(local)
    }

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

    /** 回填一条到本地，使用已记录的副属性集合重建索引（保持与远端一致）。 */
    @Suppress("UNCHECKED_CAST")
    private fun saveOneLocal(entity: IIdEntity<*>) {
        local?.save(
            name,
            entity as IIdEntity<Any?>,
            indexedFilterable,
            indexedSortable
        )
    }

    /** 回填多条到本地。优先用 saveBatch 减少 N 次单条调用的开销。 */
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
                // 旧实现 entities.forEach { pushHashNotify(it.id) }：N 条 publish 风暴。
                // 改为一条消息携带 id 列表，接收方在 RedisCacheMessageHandler 里按 Collection 展开 evictLocal。
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
                // 与 saveBatch 一致：单条通知带 id 列表，避免 N 条 Pub/Sub 风暴。
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
            // 确保被查询的 property 一定在 filterable 索引集合中，否则本地永远无法按此属性命中。
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