package io.kudos.ability.cache.common.aop.hash

import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.support.CacheConfig
import io.kudos.ability.cache.common.support.ICacheConfigProvider
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import kotlin.reflect.KClass

/**
 * Hash 切面测试公共支撑件：内存版 [IHashCache]、可编程 [ICacheConfigProvider]、
 * 测试实体，以及把假 hash cache 注入 [MixHashCacheManager] 私有 map 的反射辅助。
 *
 * @author K
 * @since 1.0.0
 */

/** 测试实体：带 id（主属性）及 type/code/score（次级属性）。 */
internal data class HashEntity(
    override val id: String?,
    val type: String? = null,
    val code: String? = null,
    val score: Int? = null
) : IIdEntity<String?>

/**
 * 内存版假 [IHashCache]：getById 走 [entities]；listBySetIndex 走可编程的 [setIndex]；
 * save/saveBatch 记录调用并写入 [entities]。其余方法测试不涉及，直接抛异常以防误用。
 */
internal class FakeHashCache : IHashCache {

    val entities = linkedMapOf<Any?, IIdEntity<*>>()
    val setIndex = mutableMapOf<Pair<String, Any>, List<IIdEntity<*>>>()
    val saveCalls = mutableListOf<Triple<IIdEntity<*>, Set<String>, Set<String>>>()
    val saveBatchCalls = mutableListOf<Triple<List<IIdEntity<*>>, Set<String>, Set<String>>>()
    var getByIdCount = 0
        private set
    var listBySetIndexCount = 0
        private set

    fun reset() {
        entities.clear()
        setIndex.clear()
        saveCalls.clear()
        saveBatchCalls.clear()
        getByIdCount = 0
        listBySetIndexCount = 0
    }

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? {
        getByIdCount++
        @Suppress("UNCHECKED_CAST")
        return entities[id] as E?
    }

    override fun existsById(cacheName: String, id: Any): Boolean = entities.containsKey(id)

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        saveCalls += Triple(entity, filterableProperties, sortableProperties)
        entities[entity.id] = entity
    }

    override fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        saveBatchCalls += Triple(entities, filterableProperties, sortableProperties)
        entities.forEach { this.entities[it.id] = it }
    }

    override fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        entities.remove(id)
    }

    override fun <E : IIdEntity<*>> findByIds(
        cacheName: String,
        ids: Collection<*>,
        entityClass: KClass<E>
    ): List<E> = throw UnsupportedOperationException("not needed in tests")

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> =
        throw UnsupportedOperationException("not needed in tests")

    override fun <PK, E : IIdEntity<PK>> listBySetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E> {
        listBySetIndexCount++
        @Suppress("UNCHECKED_CAST")
        return setIndex[property to value].orEmpty() as List<E>
    }

    override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean
    ): List<E> = throw UnsupportedOperationException("not needed in tests")

    override fun <PK, E : IIdEntity<PK>> list(
        cacheName: String,
        entityClass: KClass<E>,
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E> = throw UnsupportedOperationException("not needed in tests")

    override fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) = throw UnsupportedOperationException("not needed in tests")

    override fun clear(cacheName: String) {
        entities.clear()
        setIndex.clear()
    }
}

/** 可编程缓存配置提供者：按名注册 active / writeInTime 开关。 */
internal class ProgrammableCacheConfigProvider : ICacheConfigProvider {
    private val configs = mutableMapOf<String, CacheConfig>()

    fun register(name: String, active: Boolean = true, writeInTime: Boolean = true) {
        configs[name] = CacheConfig().apply {
            this.name = name
            this.active = active
            this.writeInTime = writeInTime
            this.hash = true
        }
    }

    override fun getCacheConfig(name: String): CacheConfig? = configs[name]
    override fun getAllCacheConfigs(): Map<String, CacheConfig> = configs
    override fun getLocalCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    override fun getRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
    override fun getLocalRemoteCacheConfigs(): Map<String, CacheConfig> = emptyMap()
}

/**
 * 构造一个 [MixHashCacheManager] 并通过反射把 (cacheName -> hashCache) 写入其私有 hashCaches，
 * 绕过完整的 Spring 装配（versionConfig 为 null 时 getHashCache 直接用原名查找）。
 */
internal fun buildHashManager(vararg caches: Pair<String, IHashCache>): MixHashCacheManager {
    val manager = MixHashCacheManager()
    val field = MixHashCacheManager::class.java.getDeclaredField("hashCaches")
    field.isAccessible = true
    @Suppress("UNCHECKED_CAST")
    val map = field.get(manager) as MutableMap<String, IHashCache>
    caches.forEach { (name, cache) -> map[name] = cache }
    return manager
}
