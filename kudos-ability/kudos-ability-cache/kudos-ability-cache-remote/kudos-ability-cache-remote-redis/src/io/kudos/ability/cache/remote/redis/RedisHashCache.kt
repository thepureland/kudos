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
 * Hash 缓存的 Redis 底层存储实现，委托 [IdEntitiesRedisHashDao]。
 *
 * 设计原则：**只负责存储**，不负责跨节点广播。
 *
 * 历史问题（已修）：
 * 之前在每个写方法里 push 一条 Pub/Sub 通知，存在两个 bug：
 * 1) cacheName 传的是 `versionConfig.getFinalCacheName(...)`（带版本前缀），但 [io.kudos.ability.cache.local.caffeine.CaffeineHashCache]
 *    的 mainData / setIndex / zsetIndex 都以"逻辑名"为 key 存（因为 MixHashCache 调 local.save 时传的是逻辑名）。
 *    接收端用前缀名查 mainData 取不到 → 这条通知实际是无效的 no-op。
 * 2) LOCAL_REMOTE 模式下 MixHashCache 也会发一条（用逻辑名），所以一次写操作发了两条 Pub/Sub，
 *    一条有效一条 no-op，纯属浪费。
 *
 * 现在的责任划分：广播由 [io.kudos.ability.cache.common.core.hash.MixHashCache] 统一负责（用逻辑名）。
 * 业务侧总是通过 [io.kudos.ability.cache.common.kit.HashCacheKit] 拿到 MixHashCache，不直接持有此类。
 *
 * @author K
 * @since 1.0.0
 */
class RedisHashCache(
    private val redisTemplates: RedisTemplates,
    private val versionConfig: CacheVersionConfig
) : IHashCache {

    private val dao = IdEntitiesRedisHashDao(redisTemplates)

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
