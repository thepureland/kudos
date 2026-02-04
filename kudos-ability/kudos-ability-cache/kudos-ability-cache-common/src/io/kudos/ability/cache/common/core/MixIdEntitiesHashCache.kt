package io.kudos.ability.cache.common.core

import io.kudos.ability.cache.common.enums.CacheStrategy
import io.kudos.ability.cache.common.notice.CacheMessage
import io.kudos.ability.cache.common.notice.ICacheMessageHandler
import io.kudos.ability.cache.common.support.IIdEntitiesHashCache
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.context.kit.SpringKit
import kotlin.reflect.KClass

/**
 * 按策略封装本地/远程 Hash 缓存的统一视图，实现 [IIdEntitiesHashCache]。
 * 读按策略委托 local/remote，写写 remote（LOCAL_REMOTE 时并发通知清理本地）。
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
internal class MixIdEntitiesHashCache(
    private val cacheName: String,
    private val strategy: CacheStrategy,
    private val local: IIdEntitiesHashCache?,
    private val remote: IIdEntitiesHashCache?,
    private val nodeId: String
) : IIdEntitiesHashCache {

    private fun remoteOrLocal(): IIdEntitiesHashCache =
        remote ?: local ?: throw IllegalStateException("Hash cache '$cacheName' has no local or remote impl")

    private val name: String = cacheName

    private fun <T> readFromLocalFirst(block: (IIdEntitiesHashCache) -> T): T? {
        if (local == null) return null
        return block(local)
    }

    private fun pushHashNotify(key: Any?) {
        val handlers = SpringKit.getBeansOfType(ICacheMessageHandler::class)
        if (handlers.isEmpty()) return
        val msg = CacheMessage(name, key).apply {
            cacheType = "hash"
            nodeId = this@MixIdEntitiesHashCache.nodeId
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
        setIndexPropertyNames: Set<String>,
        zsetIndexPropertyNames: Set<String>
    ) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.save(name, entity, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.REMOTE -> remote!!.save(name, entity, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.save(name, entity, setIndexPropertyNames, zsetIndexPropertyNames)
                pushHashNotify(entity.id)
            }
        }
    }

    override fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        setIndexPropertyNames: Set<String>,
        zsetIndexPropertyNames: Set<String>
    ) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.saveBatch(name, entities, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.REMOTE -> remote!!.saveBatch(name, entities, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.saveBatch(name, entities, setIndexPropertyNames, zsetIndexPropertyNames)
                entities.forEach { pushHashNotify(it.id) }
            }
        }
    }

    override fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        setIndexPropertyNames: Set<String>,
        zsetIndexPropertyNames: Set<String>
    ) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.deleteById(name, id, entityClass, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.REMOTE -> remote!!.deleteById(name, id, entityClass, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.deleteById(name, id, entityClass, setIndexPropertyNames, zsetIndexPropertyNames)
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
            return fromRemote
        }
        return remoteOrLocal().findByIds(name, ids, entityClass)
    }

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> {
        if (strategy == CacheStrategy.LOCAL_REMOTE) {
            val fromLocal = readFromLocalFirst { it.listAll(name, entityClass) }
            if (!fromLocal.isNullOrEmpty()) return fromLocal
            val fromRemote = remote?.listAll(name, entityClass) ?: return emptyList()
            fromRemote.forEach { e -> local?.save(name, e, emptySet(), emptySet()) }
            return fromRemote
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
            readFromLocalFirst { it.listBySetIndex(name, entityClass, property, value) }?.let { return it }
            val fromRemote = remote?.listBySetIndex(name, entityClass, property, value) ?: return emptyList()
            fromRemote.forEach { e -> local?.save<PK, E>(name, e, emptySet(), emptySet()) }
            return fromRemote
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
            readFromLocalFirst { it.listPageByZSetIndex(name, entityClass, zsetIndexName, offset, limit, desc) }?.let { return it }
            val fromRemote = remote?.listPageByZSetIndex(name, entityClass, zsetIndexName, offset, limit, desc) ?: return emptyList()
            fromRemote.forEach { e -> local?.save(name, e, emptySet(), emptySet()) }
            return fromRemote
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
            readFromLocalFirst { it.list(name, entityClass, criteria, pageNo, pageSize, *orders) }?.let { return it }
            val fromRemote = remote?.list(name, entityClass, criteria, pageNo, pageSize, *orders) ?: return emptyList()
            fromRemote.forEach { e -> local?.save(name, e, emptySet(), emptySet()) }
            return fromRemote
        }
        return remoteOrLocal().list(name, entityClass, criteria, pageNo, pageSize, *orders)
    }

    override fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        setIndexPropertyNames: Set<String>,
        zsetIndexPropertyNames: Set<String>
    ) {
        when (strategy) {
            CacheStrategy.SINGLE_LOCAL -> local!!.refreshAll(name, entities, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.REMOTE -> remote!!.refreshAll(name, entities, setIndexPropertyNames, zsetIndexPropertyNames)
            CacheStrategy.LOCAL_REMOTE -> {
                remote!!.refreshAll(name, entities, setIndexPropertyNames, zsetIndexPropertyNames)
                pushHashNotify(null)
            }
        }
    }
}
