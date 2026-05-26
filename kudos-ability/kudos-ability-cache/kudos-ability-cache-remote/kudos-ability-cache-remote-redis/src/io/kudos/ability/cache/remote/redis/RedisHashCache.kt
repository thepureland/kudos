package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.dao.IdEntitiesRedisHashDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * Redis-backed storage implementation of the Hash cache, delegating to [IdEntitiesRedisHashDao].
 *
 * Design principle: **storage only**, no cross-node broadcasting.
 *
 * History (already fixed):
 * Previously each write method pushed a Pub/Sub notification, which had two bugs:
 * 1) cacheName was passed as `versionConfig.getFinalCacheName(...)` (version-prefixed), but
 *    [io.kudos.ability.cache.local.caffeine.CaffeineHashCache] keys mainData / setIndex / zsetIndex by the
 *    "logical name" (MixHashCache passes the logical name to local.save). The receiver could not find
 *    mainData by the prefixed name, so the notification was effectively a no-op.
 * 2) Under LOCAL_REMOTE, MixHashCache also publishes one (using the logical name), so a single write
 *    produced two Pub/Sub messages — one useful, one no-op — pure waste.
 *
 * Current ownership: broadcasting is centralized in [io.kudos.ability.cache.common.core.hash.MixHashCache]
 * (using the logical name). Business code always goes through [io.kudos.ability.cache.common.kit.HashCacheKit]
 * to obtain MixHashCache and never holds this class directly.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class RedisHashCache(
    private val redisTemplates: RedisTemplates,
    private val versionConfig: CacheVersionConfig
) : IHashCache {

    /** Delegated Redis hash DAO; all reads/writes go through it via RedisTemplate. */
    private val dao = IdEntitiesRedisHashDao(redisTemplates)

    /**
     * Combines the business `cacheName` with the version prefix to form the Redis key prefix.
     * Centralized once so every method follows the same rule and avoids inconsistent computations.
     *
     * @param cacheName logical business cache name
     * @return final Redis key prefix with the version prefix applied
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun dataKeyPrefix(cacheName: String): String = versionConfig.getFinalCacheName(cacheName)

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? =
        dao.getById(dataKeyPrefix(cacheName), id, entityClass)

    override fun existsById(cacheName: String, id: Any): Boolean =
        dao.existsById(dataKeyPrefix(cacheName), id)

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.save(dataKeyPrefix(cacheName), entity, filterableProperties, sortableProperties)
    }

    override fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.saveBatch(dataKeyPrefix(cacheName), entities, filterableProperties, sortableProperties)
    }

    override fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.deleteById(dataKeyPrefix(cacheName), id, entityClass, filterableProperties, sortableProperties)
    }

    override fun <PK, E : IIdEntity<PK>> deleteByIds(
        cacheName: String,
        ids: Collection<PK>,
        entityClass: KClass<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        if (ids.isEmpty()) return
        ids.forEach { dao.deleteById(dataKeyPrefix(cacheName), it, entityClass, filterableProperties, sortableProperties) }
    }

    override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> =
        dao.findByIds(dataKeyPrefix(cacheName), ids, entityClass)

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> =
        dao.listAll(dataKeyPrefix(cacheName), entityClass)

    override fun <PK, E : IIdEntity<PK>> listBySetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E> = dao.listBySetIndex(dataKeyPrefix(cacheName), entityClass, property, value)

    override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean
    ): List<E> = dao.listPageByZSetIndex(dataKeyPrefix(cacheName), entityClass, zsetIndexName, offset, limit, desc)

    override fun <PK, E : IIdEntity<PK>> list(
        cacheName: String,
        entityClass: KClass<E>,
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E> = dao.list(dataKeyPrefix(cacheName), entityClass, criteria, pageNo, pageSize, *orders)

    override fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.refreshAll(dataKeyPrefix(cacheName), entities, filterableProperties, sortableProperties)
    }

    override fun clear(cacheName: String) {
        dao.clear(dataKeyPrefix(cacheName))
    }
}
