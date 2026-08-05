package io.kudos.ability.cache.common.core.hash

import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the default members of [IHashCache] (compiled into DefaultImpls): the default `deleteByIds`
 * loop and the default-valued `filterableProperties` / `sortableProperties` parameters on save / saveBatch /
 * deleteById / refreshAll.
 *
 * Uses a minimal implementation that does NOT override `deleteByIds`, so the interface default body runs.
 *
 * @author K
 * @since 1.0.0
 */
internal class IHashCacheDefaultsTest {

    private data class E(override val id: String) : IIdEntity<String>

    @Test
    fun defaultDeleteByIds_loopsOverDeleteById() {
        val cache = MinimalHashCache()
        cache.save<String, E>("c", E("1"))
        cache.save<String, E>("c", E("2"))
        // default deleteByIds (not overridden) -> loops deleteById
        cache.deleteByIds<String, E>("c", listOf("1", "2"), E::class)
        assertTrue(cache.store.isEmpty())
        assertEquals(listOf<Any?>("1", "2"), cache.deletedIds)
    }

    @Test
    fun defaultParameters_onWriteMethods_useEmptySets() {
        val cache = MinimalHashCache()
        // call with omitted filterable/sortable -> default emptySet()
        cache.save<String, E>("c", E("1"))
        cache.saveBatch<String, E>("c", listOf(E("2")))
        cache.deleteById<String, E>("c", "2", E::class)
        cache.refreshAll<String, E>("c", listOf(E("3")))
        assertEquals(setOf<Any?>("1", "3"), cache.store.keys)
        assertTrue(cache.lastFilterable.isEmpty())
        assertTrue(cache.lastSortable.isEmpty())
    }

    @Test
    fun listPageByZSetIndex_defaultDescParam() {
        // omit the trailing desc parameter (default true) -> exercises the synthetic default-arg bridge
        val cache = MinimalHashCache()
        assertTrue(cache.listPageByZSetIndex<String, E>("c", E::class, "z", 0, 10).isEmpty())
    }

    /** Minimal IHashCache; keeps the default deleteByIds. */
    private class MinimalHashCache : IHashCache {
        val store = linkedMapOf<Any?, IIdEntity<*>>()
        val deletedIds = mutableListOf<Any?>()
        var lastFilterable: Set<String> = setOf("dirty")
        var lastSortable: Set<String> = setOf("dirty")

        override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? {
            @Suppress("UNCHECKED_CAST") return store[id] as E?
        }
        override fun existsById(cacheName: String, id: Any): Boolean = store.containsKey(id)
        override fun <PK, E : IIdEntity<PK>> save(cacheName: String, entity: E, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            lastFilterable = filterableProperties; lastSortable = sortableProperties; store[entity.id] = entity
        }
        override fun <PK, E : IIdEntity<PK>> saveBatch(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            lastFilterable = filterableProperties; lastSortable = sortableProperties; entities.forEach { store[it.id] = it }
        }
        override fun <PK, E : IIdEntity<PK>> deleteById(cacheName: String, id: PK, entityClass: KClass<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            deletedIds.add(id); store.remove(id)
        }
        @Suppress("UNCHECKED_CAST")
        override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> =
            ids.mapNotNull { store[it] } as List<E>
        @Suppress("UNCHECKED_CAST")
        override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> =
            store.values.toList() as List<E>
        override fun <PK, E : IIdEntity<PK>> listBySetIndex(cacheName: String, entityClass: KClass<E>, property: String, value: Any): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(cacheName: String, entityClass: KClass<E>, zsetIndexName: String, offset: Long, limit: Long, desc: Boolean): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> list(cacheName: String, entityClass: KClass<E>, criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> = emptyList()
        override fun <PK, E : IIdEntity<PK>> refreshAll(cacheName: String, entities: List<E>, filterableProperties: Set<String>, sortableProperties: Set<String>) {
            lastFilterable = filterableProperties; lastSortable = sortableProperties; entities.forEach { store[it.id] = it }
        }
        override fun clear(cacheName: String) { store.clear() }
    }
}
