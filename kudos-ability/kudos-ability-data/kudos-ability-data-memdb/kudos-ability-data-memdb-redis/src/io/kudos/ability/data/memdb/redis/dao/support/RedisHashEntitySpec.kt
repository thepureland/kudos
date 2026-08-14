package io.kudos.ability.data.memdb.redis.dao.support

import io.kudos.base.model.contract.entity.IIdEntity
import kotlin.reflect.KClass

/**
 * Describes how one entity type is laid out in Redis: which key holds it, what it deserializes to, and which
 * properties are indexed.
 *
 * Exists because [io.kudos.ability.data.memdb.redis.dao.IdEntitiesRedisHashDao] takes all of that per call.
 * That is not just verbose — the index property sets are a **correctness** requirement: `save` and `deleteById`
 * must be handed the same sets, otherwise the delete leaves index entries pointing at a row that is gone, and
 * nothing in the type system catches the mismatch. Binding them once removes the chance to disagree.
 *
 * Pair it with [io.kudos.ability.data.memdb.redis.dao.BoundIdEntitiesRedisHashDao]:
 * ```kotlin
 * val spec = RedisHashEntitySpec(
 *     dataKeyPrefix = "sys:dict",
 *     entityClass = Dict::class,
 *     filterableProperties = setOf("type", "active"),
 *     sortableProperties = setOf("updateTime"),
 * )
 * val dao = BoundIdEntitiesRedisHashDao(redisTemplates, spec)
 * dao.save(dict)          // indexes maintained per the spec
 * dao.deleteById("d1")    // ...and removed per the same spec
 * ```
 *
 * @param PK primary key type of the entity
 * @param E entity type
 * @property dataKeyPrefix Redis key of the Hash holding the rows; index keys are derived from it.
 *                         On Redis Cluster give it a hash tag (e.g. `{sysDict}:rows`) so the main key and its
 *                         index keys land in one slot — the multi-key index operations require that.
 * @property entityClass Row type, used to deserialize values read back.
 * @property filterableProperties Properties indexed as Sets for equality / IN queries.
 * @property sortableProperties Properties indexed as ZSets for sorting / range queries; their values must be numeric.
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class RedisHashEntitySpec<PK : Any, E : IIdEntity<PK>>(
    val dataKeyPrefix: String,
    val entityClass: KClass<E>,
    val filterableProperties: Set<String> = emptySet(),
    val sortableProperties: Set<String> = emptySet(),
) {

    init {
        require(dataKeyPrefix.isNotBlank()) { "dataKeyPrefix must not be blank" }
    }

}
