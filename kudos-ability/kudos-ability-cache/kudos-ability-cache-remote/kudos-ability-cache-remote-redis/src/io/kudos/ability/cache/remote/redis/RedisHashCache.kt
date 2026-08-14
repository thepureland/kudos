package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.dao.IdEntitiesRedisHashDao
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.Duration
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
    private val versionConfig: CacheVersionConfig,
    /**
     * Resolves the configured `ttl` (seconds) for a logical cache name, or null for "never expires".
     * Consulted per write rather than captured once, so it works regardless of when the cache configuration
     * becomes available relative to this bean.
     */
    private val ttlSecondsOf: (cacheName: String) -> Int? = { null }
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
        applyTtl(cacheName)
    }

    override fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.saveBatch(dataKeyPrefix(cacheName), entities, filterableProperties, sortableProperties)
        applyTtl(cacheName)
    }

    /**
     * Re-applies the cache item's configured `ttl` to every key of this hash region after a write.
     *
     * Remote hash caches had no expiry at all, which made `CacheConfig.ttl` silently inert for `hash = true`
     * items and left nothing to converge them: cross-node invalidation is fire-and-forget pub/sub, so a single
     * dropped message meant stale data resident forever.
     *
     * The TTL covers the main key *and* every index key together (see
     * [io.kudos.ability.data.memdb.redis.dao.IdEntitiesRedisHashDao.expireAll]) — expiring only some of them
     * would leave indexes pointing at entities that no longer exist. Because it is re-applied on each write, the
     * region lives for `ttl` measured from the **last write**, not from when it was first populated.
     *
     * Two consequences worth knowing before configuring a ttl on a hash cache:
     *  - it costs a SCAN over this region's index keys per write, so it stays entirely out of the way when no
     *    ttl is configured (the common case) and should be sized with write frequency in mind;
     *  - a hash cache has no load-on-miss path, so once the region expires, queries return empty until
     *    something repopulates it (`writeOnBoot` preloading, an explicit `reloadAll`, or ordinary writes).
     *    A ttl here bounds staleness; it does not arrange for a refresh.
     */
    private fun applyTtl(cacheName: String) {
        val ttlSeconds = runCatching { ttlSecondsOf(cacheName) }.getOrNull() ?: return
        if (ttlSeconds <= 0) return
        runCatching { dao.expireAll(dataKeyPrefix(cacheName), Duration.ofSeconds(ttlSeconds.toLong())) }
            .onFailure {
                // A failed expiry must not fail the write that already succeeded; the next write retries it.
                log.warn(
                    "Failed to apply ttl to hash cache [{0}]; entries keep their previous expiry. cause={1}",
                    cacheName, it.message
                )
            }
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
        applyTtl(cacheName)
    }

    override fun clear(cacheName: String) {
        dao.clear(dataKeyPrefix(cacheName))
    }

    private val log = LogFactory.getLog(this::class)
}
