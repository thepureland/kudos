package io.kudos.ability.cache.local.caffeine

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.kudos.ability.cache.common.core.hash.IHashCache
import io.kudos.ability.cache.common.support.IHashCacheSync
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

/**
 * Caffeine-based local implementation of hash cache; the in-memory layout mimics Redis
 * Hash + Set + ZSet. Also implements [IHashCacheSync] so the local cache can be cleared after
 * receiving a Redis notification.
 *
 * Main data is backed by Caffeine, bucketed by cacheName with a [maximumSize] limit. On eviction,
 * deletion, or overwrite, the Set/ZSet secondary indexes are kept in sync so they cannot return
 * stale ids whose main entries no longer exist.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class CaffeineHashCache(
    private val maximumSize: Long = DEFAULT_MAXIMUM_SIZE
) : IHashCache, IHashCacheSync {

    init {
        require(maximumSize > 0) { "maximumSize must be positive" }
    }

    /** Main data: cacheName -> Caffeine(id -> entity); mimics a Redis hash and provides a capacity limit */
    private val mainData = ConcurrentHashMap<String, Cache<String, Any>>()
    /** Set secondary index: cacheName -> (propertyKey -> ids); mimics a Redis set, used for equality lookup by property */
    private val setIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    /** ZSet secondary index: cacheName -> (propertyKey -> (id -> score)); mimics a Redis zset, used for sort/range by property */
    private val zsetIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableMap<String, Double>>>()

    /** Get / lazily create the main data space */
    private fun main(cacheName: String): Cache<String, Any> =
        mainData.computeIfAbsent(cacheName) {
            Caffeine.newBuilder()
                .maximumSize(maximumSize)
                .executor(Runnable::run)
                .removalListener<String, Any> { id, _, _ ->
                    if (id != null) {
                        removeFromIndexes(cacheName, id)
                    }
                }
                .build()
        }

    /** Get / lazily create the Set index space */
    private fun setIdx(cacheName: String) = setIndex.getOrPut(cacheName) { ConcurrentHashMap() }
    /** Get / lazily create the ZSet index space */
    private fun zsetIdx(cacheName: String) = zsetIndex.getOrPut(cacheName) { ConcurrentHashMap() }

    /** Composite key for the Set index: avoids naming conflicts across different properties */
    private fun setKey(property: String, value: String) = "set:$property:$value"
    /** Composite key for the ZSet index: same as above, a single-layer zset per property */
    private fun zsetKey(property: String) = "zset:$property"

    /**
     * Remove the given id from all secondary indexes and clean up empty index buckets.
     */
    private fun removeFromIndexes(cacheName: String, id: String) {
        setIndex[cacheName]?.entries?.removeIf { (_, ids) ->
            ids.remove(id)
            ids.isEmpty()
        }
        zsetIndex[cacheName]?.entries?.removeIf { (_, idToScore) ->
            idToScore.remove(id)
            idToScore.isEmpty()
        }
    }

    /**
     * Canonical form of the primary key used in Hash fields / indexes, so trailing whitespace from
     * types like CHAR cannot diverge from the trimmed PK supplied by callers and miss the cache.
     */
    private fun normalizePkField(id: Any?): String = (id ?: "").toString().trim()

    /**
     * Convert any value into a Double for use as a zset score.
     * Non-numeric strings or other types fall back to `-Double.MAX_VALUE` — not `Double.MIN_VALUE`,
     * which is the smallest positive value and would mis-order negative scores.
     *
     * @param value any value
     * @return Double score
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        // -Double.MAX_VALUE is the most negative value; Double.MIN_VALUE is actually the smallest
        // positive value and, used as a lower-bound fallback, would push members with negative
        // scores to the front (wrong order).
        is String -> value.toDoubleOrNull() ?: -Double.MAX_VALUE
        else -> -Double.MAX_VALUE
    }

    /**
     * Reflectively get the value of the given property on an entity.
     * Order: field (setAccessible) -> `getXxx` -> `isXxx`; returns null if none of them exists.
     * Failures are swallowed in try-catch; callers already have fallbacks for null.
     *
     * @param entity target object
     * @param propertyName property name
     * @return the property value; null if it cannot be resolved
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun getPropertyValue(entity: Any, propertyName: String): Any? {
        val clazz = entity.javaClass
        try {
            val field = clazz.getDeclaredField(propertyName)
            field.isAccessible = true
            return field.get(entity)
        } catch (_: NoSuchFieldException) { }
        val getterName = "get" + propertyName.replaceFirstChar { it.uppercase() }
        try {
            val method: Method = clazz.getMethod(getterName)
            return method.invoke(entity)
        } catch (_: NoSuchMethodException) { }
        val boolGetterName = "is" + propertyName.replaceFirstChar { it.uppercase() }
        try {
            val method: Method = clazz.getMethod(boolGetterName)
            return method.invoke(entity)
        } catch (_: NoSuchMethodException) { }
        return null
    }

    override fun clearLocal(cacheName: String) {
        mainData.remove(cacheName)?.let {
            it.invalidateAll()
            it.cleanUp()
        }
        setIndex.remove(cacheName)
        zsetIndex.remove(cacheName)
    }

    override fun evictLocal(cacheName: String, id: Any) {
        val idStr = normalizePkField(id)
        val cache = mainData[cacheName] ?: return
        cache.invalidate(idStr)
        cache.cleanUp()
        removeFromIndexes(cacheName, idStr)
    }

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? {
        @Suppress("UNCHECKED_CAST")
        return main(cacheName).getIfPresent(normalizePkField(id)) as? E
    }

    override fun existsById(cacheName: String, id: Any): Boolean =
        main(cacheName).getIfPresent(normalizePkField(id)) != null

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        val id = entity.id ?: throw IllegalArgumentException("entity.id must not be null")
        val idStr = normalizePkField(id)
        removeFromIndexes(cacheName, idStr)
        val cache = main(cacheName)
        cache.put(idStr, entity)
        filterableProperties.forEach { prop ->
            getPropertyValue(entity, prop)?.let { value ->
                setIdx(cacheName).getOrPut(setKey(prop, value.toString())) { ConcurrentHashMap.newKeySet() }.add(idStr)
            }
        }
        sortableProperties.forEach { prop ->
            getPropertyValue(entity, prop)?.let { value ->
                zsetIdx(cacheName).getOrPut(zsetKey(prop)) { ConcurrentHashMap() }[idStr] = toDouble(value)
            }
        }
        cache.cleanUp()
    }

    override fun <PK, E : IIdEntity<PK>> saveBatch(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        entities.forEach { save(cacheName, it, filterableProperties, sortableProperties) }
    }

    override fun <PK, E : IIdEntity<PK>> deleteById(
        cacheName: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        val idStr = normalizePkField(id)
        val cache = mainData[cacheName] ?: return
        cache.invalidate(idStr)
        cache.cleanUp()
        removeFromIndexes(cacheName, idStr)
    }

    override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> {
        if (ids.isEmpty()) return emptyList()
        val cache = main(cacheName)
        return ids.mapNotNull {
            @Suppress("UNCHECKED_CAST")
            cache.getIfPresent(normalizePkField(it)) as? E
        }
    }

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> {
        @Suppress("UNCHECKED_CAST")
        return main(cacheName).asMap().values.toList().map { it as E }
    }

    override fun <PK, E : IIdEntity<PK>> listBySetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E> {
        val ids = setIdx(cacheName)[setKey(property, value.toString())]?.toList() ?: emptyList()
        return findByIds(cacheName, ids, entityClass)
    }

    override fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
        cacheName: String,
        entityClass: KClass<E>,
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean
    ): List<E> {
        val idToScore = zsetIdx(cacheName)[zsetKey(zsetIndexName)] ?: return emptyList()
        val ordered = if (desc) idToScore.entries.sortedByDescending { it.value } else idToScore.entries.sortedBy { it.value }
        val pageIds = ordered.drop(offset.toInt()).take(limit.toInt()).map { it.key }
        return findByIds(cacheName, pageIds, entityClass)
    }

    override fun <PK, E : IIdEntity<PK>> list(
        cacheName: String,
        entityClass: KClass<E>,
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E> {
        var entities = listAll(cacheName, entityClass)
        if (criteria != null && !criteria.isEmpty()) {
            entities = entities.filter { matchesCriteria(it, criteria) }
        }
        if (orders.isNotEmpty()) {
            val first = orders.first()
            val prop = first.property
            val desc = first.direction == DirectionEnum.DESC
            // Same as toDouble: use -Double.MAX_VALUE as the lower-bound fallback so negative values are not mis-ordered
            val fallback = -Double.MAX_VALUE
            entities = if (desc) entities.sortedByDescending { getPropertyValue(it, prop)?.let { v -> toDouble(v) } ?: fallback }
            else entities.sortedBy { getPropertyValue(it, prop)?.let { v -> toDouble(v) } ?: fallback }
        }
        val pNo = if (pageNo < 1) 1 else pageNo
        val pSize = if (pageSize < 1) 1 else pageSize
        val offset = (pNo - 1) * pSize
        return entities.drop(offset).take(pSize)
    }

    private fun matchesCriteria(entity: Any, criteria: Criteria): Boolean {
        for (group in criteria.getCriterionGroups()) {
            when (group) {
                is Criterion -> if (!matchesCriterion(entity, group)) return false
                is Array<*> -> if (!group.any { (it is Criterion && matchesCriterion(entity, it)) || (it is Criteria && matchesCriteria(entity, it)) }) return false
                is Criteria -> if (!matchesCriteria(entity, group)) return false
            }
        }
        return true
    }

    private fun matchesCriterion(entity: Any, c: Criterion): Boolean {
        val actual = getPropertyValue(entity, c.property) ?: return c.operator.acceptNull
        val expected = c.value ?: return c.operator.acceptNull
        return when (c.operator) {
            OperatorEnum.EQ -> actual.toString() == expected.toString()
            OperatorEnum.IN -> (expected as? Collection<*>)?.any { actual.toString() == it.toString() } == true
            OperatorEnum.GT -> toDouble(actual) > toDouble(expected)
            OperatorEnum.GE -> toDouble(actual) >= toDouble(expected)
            OperatorEnum.LT -> toDouble(actual) < toDouble(expected)
            OperatorEnum.LE -> toDouble(actual) <= toDouble(expected)
            else -> actual.toString() == expected.toString()
        }
    }

    override fun <PK, E : IIdEntity<PK>> refreshAll(
        cacheName: String,
        entities: List<E>,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        clearLocal(cacheName)
        entities.forEach { save(cacheName, it, filterableProperties, sortableProperties) }
    }

    override fun clear(cacheName: String) {
        clearLocal(cacheName)
    }

    companion object {
        const val DEFAULT_MAXIMUM_SIZE = 10_000L
    }
}
