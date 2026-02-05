package io.kudos.ability.cache.common.core.hash

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
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

    private fun <T> readFromLocalFirst(block: (IHashCache) -> T): T? {
        if (local == null) return null
        return block(local)
    }

    private fun pushHashNotify(key: Any?) {
        val handlers = SpringKit.getBeansOfType(ICacheMessageHandler::class)
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
            local?.save(name, fromRemote, emptySet(), emptySet())
            return fromRemote
        }
        return remoteOrLocal().getById(name, id, entityClass)
    }

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.save(name, entity, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> remote!!.save(name, entity, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.save(name, entity, filterableProperties, sortableProperties)
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
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.saveBatch(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> remote!!.saveBatch(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.saveBatch(name, entities, filterableProperties, sortableProperties)
                local?.saveBatch(name, entities, filterableProperties, sortableProperties)
                entities.forEach { pushHashNotify(it.id) }
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
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> remote!!.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
                local?.deleteById(name, id, entityClass, filterableProperties, sortableProperties)
                pushHashNotify(id)
            }
        }
    }

    override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.findByIds(name, ids, entityClass) }
            if (fromLocal != null && fromLocal.size == ids.size) return fromLocal
            val fromRemote = remote?.findByIds(name, ids, entityClass) ?: return emptyList()
            fromRemote.forEach { e ->
                @Suppress("UNCHECKED_CAST")
                fun writeOne(entity: IIdEntity<Any?>) {
                    local?.save(name, entity, emptySet(), emptySet())
                }
                @Suppress("UNCHECKED_CAST")
                writeOne(e as IIdEntity<Any?>)
            }
            return local?.findByIds(name, ids, entityClass) ?: fromRemote
        }
        return remoteOrLocal().findByIds(name, ids, entityClass)
    }

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.listAll(name, entityClass) }
            if (!fromLocal.isNullOrEmpty()) return fromLocal
            val fromRemote = remote?.listAll(name, entityClass) ?: return emptyList()
            fromRemote.forEach { e -> local?.save(name, e, emptySet(), emptySet()) }
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
            fromRemote.forEach { e -> local?.save(name, e, setOf(property), emptySet()) }
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
            fromRemote.forEach { e -> local?.save(name, e, emptySet(), setOf(zsetIndexName)) }
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
            fromRemote.forEach { e -> local?.save(name, e, emptySet(), emptySet()) }
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
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.refreshAll(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.REMOTE -> remote!!.refreshAll(name, entities, filterableProperties, sortableProperties)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.refreshAll(name, entities, filterableProperties, sortableProperties)
                local?.refreshAll(name, entities, filterableProperties, sortableProperties)
                pushHashNotify(null)
            }
        }
    }
}