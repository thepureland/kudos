package io.kudos.ability.data.memdb.redis.dao

import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.dao.support.RedisHashEntitySpec
import io.kudos.base.model.contract.entity.IIdEntity
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.Order
import java.time.Duration

/**
 * Type-safe facade over [IdEntitiesRedisHashDao] for a single entity type, bound to one [RedisHashEntitySpec].
 *
 * The underlying DAO takes the key prefix, the entity class and both index property sets on every call; here
 * they are supplied once by the spec. Besides the shorter call sites, this removes the failure mode where
 * `save` and `deleteById` are handed different index property sets and the indexes silently rot.
 *
 * Composition, not inheritance: the untyped API stays reachable only through the [delegate] you pass in, so a
 * caller cannot accidentally bypass the spec on this object. Subclasses of [IdEntitiesRedisHashDao] (e.g. ones
 * overriding `getRedisTemplate()` to use a non-default Redis instance) can be bound by passing them as the
 * delegate.
 *
 * @param PK primary key type
 * @param E entity type
 * @property spec layout and index description of the entity
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class BoundIdEntitiesRedisHashDao<PK : Any, E : IIdEntity<PK>>(
    protected val delegate: IdEntitiesRedisHashDao,
    val spec: RedisHashEntitySpec<PK, E>,
) {

    /** Binds the spec to a DAO over the default Redis instance. */
    constructor(redisTemplates: RedisTemplates, spec: RedisHashEntitySpec<PK, E>) :
            this(IdEntitiesRedisHashDao(redisTemplates), spec)

    // ---------- Single-row CRUD ----------

    /** Saves or updates one row and maintains the indexes declared by the spec. */
    open fun save(entity: E) =
        delegate.save(spec.dataKeyPrefix, entity, spec.filterableProperties, spec.sortableProperties)

    /** Batch save / update; main data is written through a pipeline. */
    open fun saveBatch(entities: List<E>) =
        delegate.saveBatch(spec.dataKeyPrefix, entities, spec.filterableProperties, spec.sortableProperties)

    /** Reads one row by id, or null when absent. */
    open fun getById(id: PK): E? = delegate.getById(spec.dataKeyPrefix, id, spec.entityClass)

    /** HEXISTS check that does not deserialize the value. */
    open fun existsById(id: PK): Boolean = delegate.existsById(spec.dataKeyPrefix, id)

    /** Deletes one row and removes it from the indexes declared by the spec. */
    open fun deleteById(id: PK) =
        delegate.deleteById(spec.dataKeyPrefix, id, spec.entityClass, spec.filterableProperties, spec.sortableProperties)

    /** Batch read by ids; missing / unparseable rows are skipped. */
    open fun findByIds(ids: Collection<PK>): List<E> =
        delegate.findByIds(spec.dataKeyPrefix, ids, spec.entityClass)

    // ---------- Full table and pagination ----------

    /** Reads every row (no pagination). */
    open fun listAll(): List<E> = delegate.listAll(spec.dataKeyPrefix, spec.entityClass)

    /** Equality query over a Set index. */
    open fun listBySetIndex(property: String, value: Any): List<E> =
        delegate.listBySetIndex(spec.dataKeyPrefix, spec.entityClass, property, value)

    /** Paged read straight off a ZSet index. */
    open fun listPageByZSetIndex(
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean = true
    ): List<E> = delegate.listPageByZSetIndex(spec.dataKeyPrefix, spec.entityClass, zsetIndexName, offset, limit, desc)

    /** Criteria + pagination + sorting; sorting and paging are performed inside Redis. */
    open fun list(criteria: Criteria?, pageNo: Int, pageSize: Int, vararg orders: Order): List<E> =
        delegate.list(spec.dataKeyPrefix, spec.entityClass, criteria, pageNo, pageSize, *orders)

    // ---------- Whole-table maintenance ----------

    /** Full refresh with per-key atomic swap; indexes are rebuilt from the spec. */
    open fun refreshAll(entities: List<E>) =
        delegate.refreshAll(spec.dataKeyPrefix, entities, spec.filterableProperties, spec.sortableProperties)

    /** Removes the main data and every index key of this entity. */
    open fun clear() = delegate.clear(spec.dataKeyPrefix)

    /** Applies the same TTL to the main key and every index key. */
    open fun expireAll(ttl: Duration) = delegate.expireAll(spec.dataKeyPrefix, ttl)

}
