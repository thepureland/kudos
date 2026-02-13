package io.kudos.ability.cache.remote.redis

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.init.properties.CacheVersionConfig
import io.kudos.ability.cache.common.notify.CacheMessage
import io.kudos.ability.cache.common.notify.ICacheMessageHandler
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.dao.IdEntitiesRedisHashDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import io.kudos.context.kit.SpringKit
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.beans.factory.annotation.Qualifier
import kotlin.reflect.KClass

/**
 * Hash 缓存的 Redis 底层实现，委托 [IdEntitiesRedisHashDao]；
 * 写操作后发 Redis 通知（cacheType=hash）供各节点清理本地。
 *
 * @author K
 * @since 1.0.0
 */
class RedisHashCache(
    @Autowired
    private val redisTemplates: RedisTemplates,
    private val versionConfig: CacheVersionConfig,
    @Autowired(required = false) @Qualifier("redisCacheMessageHandler") private val messageHandler: ICacheMessageHandler? = null,
    @Autowired(required = false) @Qualifier("cacheNodeId") private val nodeId: String? = null
) : IHashCache {

    private val dao = IdEntitiesRedisHashDao(redisTemplates)

    private fun dataKeyPrefix(cacheName: String): String = versionConfig.getFinalCacheName(cacheName)

    private fun pushHashNotify(cacheName: String, key: Any?) {
        val handler = messageHandler ?: SpringKit.getBeansOfType<ICacheMessageHandler>().values.firstOrNull() ?: return
        val msg = CacheMessage(versionConfig.getFinalCacheName(cacheName), key).apply {
            cacheType = "hash"
            this.nodeId = this@RedisHashCache.nodeId
        }
        handler.sendMessage(msg)
    }

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? =
        dao.getById(dataKeyPrefix(cacheName), id, entityClass)

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.save(dataKeyPrefix(cacheName), entity, filterableProperties, sortableProperties)
        pushHashNotify(cacheName, entity.id)
    }

    override fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.saveBatch(dataKeyPrefix(cacheName), entities, filterableProperties, sortableProperties)
        entities.forEach { pushHashNotify(cacheName, it.id) }
    }

    override fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        dao.deleteById(dataKeyPrefix(cacheName), id, entityClass, filterableProperties, sortableProperties)
        pushHashNotify(cacheName, id)
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
        pushHashNotify(cacheName, null)
    }
}
