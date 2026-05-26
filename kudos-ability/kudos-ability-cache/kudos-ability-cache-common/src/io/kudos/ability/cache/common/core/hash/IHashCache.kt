package io.kudos.ability.cache.common.core.hash

import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * Hash-based "id-keyed entity collection" cache interface (a single abstraction; local and remote variants
 * are delegated to by the strategy wrapper layer).
 *
 * Terminology: **primary property** is the entity's unique id, used for getById/save/deleteById;
 * **secondary properties** are non-id properties used for secondary indexes, list queries, and ordering
 * (e.g. type, status, sortScore). [filterableProperties] are Set indexes for equality filtering;
 * [sortableProperties] are ZSet indexes for ordering/range queries. Exception: numeric range-query
 * conditions must go into sortableProperties.
 * The data does not have to come from a database table — any [IIdEntity] will do; supports access by
 * the primary property, indexing/querying by secondary properties, conditional pagination/ordering, and full refresh.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IHashCache {

    fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E?

    /**
     * Lightweight check for whether the given id exists in the hash cache (does not deserialize the value).
     *
     * @param cacheName cache name
     * @param id        entity id
     * @return true: present; false: absent
     */
    fun existsById(cacheName: String, id: Any): Boolean

    /**
     * Saves an entity; [filterableProperties]/[sortableProperties] are secondary-property name sets used to
     * build Set/ZSet secondary indexes. Numeric range-query conditions go into sortableProperties.
     */
    fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )

    /**
     * Batch save; [filterableProperties]/[sortableProperties] are secondary-property name sets used to
     * build Set/ZSet secondary indexes. Numeric range-query conditions go into sortableProperties.
     */
    fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )

    /**
     * Deletes by primary id; [filterableProperties]/[sortableProperties] must match those used on write
     * so that the entry can be removed from secondary indexes.
     */
    fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )

    /**
     * Batch delete by id. The default implementation loops over [deleteById];
     * the mixed implementation (MixHashCache) overrides this to collapse into a single Pub/Sub notification,
     * avoiding an N+1 storm.
     * The underlying storage implementations (Caffeine / Redis) can keep the default loop — they are not responsible for broadcasting.
     * [filterableProperties]/[sortableProperties] are the same as in [deleteById].
     */
    fun <PK, E : IIdEntity<PK>> deleteByIds(
        cacheName: String,
        ids: Collection<PK>,
        entityClass: KClass<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    ) {
        ids.forEach { deleteById(cacheName, it, entityClass, filterableProperties, sortableProperties) }
    }

    fun <E : IIdEntity<*>> findByIds(
        cacheName: String,
        ids: Collection<*>,
        entityClass: KClass<E>
    ): List<E>

    fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E>

    /** Equality query by secondary property (Set index): [property] is the secondary property name, [value] is the property value. */
    fun <PK, E : IIdEntity<PK>> listBySetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E>

    fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean = true
    ): List<E>

    fun <PK, E : IIdEntity<PK>> list(
        cacheName: String,
        entityClass: KClass<E>,
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E>

    /**
     * Full refresh; [filterableProperties]/[sortableProperties] are secondary-property name sets used to
     * rebuild Set/ZSet indexes.
     */
    fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    )

    /**
     * Clears all data for the cache (primary data and secondary indexes).
     */
    fun clear(cacheName: String)
}