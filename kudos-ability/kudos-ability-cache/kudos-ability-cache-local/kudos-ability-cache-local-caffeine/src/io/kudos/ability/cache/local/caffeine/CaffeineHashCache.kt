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
import io.kudos.base.logger.LogFactory
import io.kudos.base.model.contract.entity.IIdEntity
import java.lang.reflect.Method
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.locks.ReentrantLock
import kotlin.concurrent.withLock
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
    private val maximumSize: Long = DEFAULT_MAXIMUM_SIZE,
    private val ttlSecondsOf: (cacheName: String) -> Int? = { null }
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

    /**
     * Get / lazily create the main data space.
     *
     * Applies the cache item's configured `ttl` as `expireAfterWrite`. Hash caches used to be bounded by size
     * only, which made `CacheConfig.ttl` silently inert for any item with `hash = true` — entries lived until
     * they were evicted for capacity or explicitly invalidated, so a dropped invalidation broadcast (pub/sub is
     * fire-and-forget) left stale data resident indefinitely with nothing to converge it.
     */
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
                .also { builder ->
                    val ttl = runCatching { ttlSecondsOf(cacheName) }.getOrNull()
                    if (ttl != null && ttl > 0) {
                        builder.expireAfterWrite(Duration.ofSeconds(ttl.toLong()))
                    }
                }
                .build()
        }

    /** Get / lazily create the Set index space */
    private fun setIdx(cacheName: String) = setIndex.getOrPut(cacheName) { ConcurrentHashMap() }
    /** Get / lazily create the ZSet index space */
    private fun zsetIdx(cacheName: String) = zsetIndex.getOrPut(cacheName) { ConcurrentHashMap() }

    /**
     * Stripe locks serializing the multi-step write of one entity.
     *
     * Writing an entity is "drop its old index entries, store it, rebuild its index entries" — three steps that
     * are individually atomic but not collectively so. Two concurrent saves of the *same* id could interleave as
     * remove(A), remove(B), add(B), add(A) and leave the entity indexed under both its old and its new property
     * value, so a secondary-property query would keep returning it under a value it no longer has.
     *
     * Striping by `cacheName + id` keeps writes to different entities fully parallel, which matters because this
     * is a local cache on the request path. A fixed array is used rather than a per-id map so the lock set
     * cannot grow with the key space (an entry per id would be a leak, and reference-counting them to allow
     * removal buys nothing here).
     */
    private val writeLocks = Array(WRITE_LOCK_STRIPES) { ReentrantLock() }

    /**
     * The lock guarding writes to one entity.
     *
     * `Math.floorMod` rather than `%`: `hashCode` may be negative, and a negative remainder would blow up on
     * array access.
     */
    private fun writeLockFor(cacheName: String, id: String): ReentrantLock =
        writeLocks[Math.floorMod(31 * cacheName.hashCode() + id.hashCode(), WRITE_LOCK_STRIPES)]

    /** Composite key for the Set index: avoids naming conflicts across different properties */
    private fun setKey(property: String, value: String) = "set:$property:$value"
    /** Composite key for the ZSet index: same as above, a single-layer zset per property */
    private fun zsetKey(property: String) = "zset:$property"

    /**
     * Remove the given id from all secondary indexes and clean up empty index buckets.
     *
     * Each bucket is updated through [ConcurrentHashMap.computeIfPresent], which holds that key's bin lock for
     * the duration of the remapping. That is what makes "remove the id, and drop the bucket if it is now empty"
     * atomic *with respect to another thread adding to the same bucket* — the add in [addToSetIndex] goes
     * through `compute` on the same key and therefore cannot interleave.
     *
     * The previous implementation used `entries.removeIf { ids.remove(id); ids.isEmpty() }`. `removeIf` does not
     * hold any lock across the predicate, so a concurrent `save` that recreated the bucket and added its id in
     * the window between "the predicate saw it empty" and "the entry was removed" lost that id outright: the
     * entity stayed in the main data but disappeared from the secondary index, and `listBySetIndex` stopped
     * returning it until something wrote it again.
     */
    private fun removeFromIndexes(cacheName: String, id: String) {
        setIndex[cacheName]?.let { buckets ->
            // Snapshot the key set: computeIfPresent may drop entries, and mutating while iterating the live
            // key view would be unsafe.
            buckets.keys.toList().forEach { key ->
                buckets.computeIfPresent(key) { _, ids ->
                    ids.remove(id)
                    if (ids.isEmpty()) null else ids
                }
            }
        }
        zsetIndex[cacheName]?.let { buckets ->
            buckets.keys.toList().forEach { key ->
                buckets.computeIfPresent(key) { _, idToScore ->
                    idToScore.remove(id)
                    if (idToScore.isEmpty()) null else idToScore
                }
            }
        }
    }

    /** Adds [id] to a Set-index bucket atomically with respect to [removeFromIndexes]. */
    private fun addToSetIndex(cacheName: String, property: String, value: String, id: String) {
        setIdx(cacheName).compute(setKey(property, value)) { _, existing ->
            (existing ?: ConcurrentHashMap.newKeySet()).apply { add(id) }
        }
    }

    /** Adds [id] to a ZSet-index bucket atomically with respect to [removeFromIndexes]. */
    private fun addToZSetIndex(cacheName: String, property: String, id: String, score: Double) {
        zsetIdx(cacheName).compute(zsetKey(property)) { _, existing ->
            (existing ?: ConcurrentHashMap<String, Double>()).apply { put(id, score) }
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
        // Empty the existing instances in place rather than detaching them from the maps. `mainData.remove(...)`
        // left any concurrent writer holding the old Cache reference writing into an object nothing reads from
        // any more — `main()` would then build a fresh empty one, and those writes vanished without a trace.
        // Clearing in place keeps every participant on the same instance.
        mainData[cacheName]?.let {
            it.invalidateAll()
            it.cleanUp()
        }
        setIndex[cacheName]?.clear()
        zsetIndex[cacheName]?.clear()
    }

    override fun evictLocal(cacheName: String, id: Any) {
        val idStr = normalizePkField(id)
        writeLockFor(cacheName, idStr).withLock {
            val cache = mainData[cacheName] ?: return
            cache.invalidate(idStr)
            cache.cleanUp()
            removeFromIndexes(cacheName, idStr)
        }
    }

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? {
        val stored = main(cacheName).getIfPresent(normalizePkField(id)) ?: return null
        return asEntity(cacheName, stored, entityClass)
    }

    /**
     * Narrows a stored value to [entityClass], or reports a miss if it is something else.
     *
     * `entityClass` used to be ignored outright — the casts here were `as? E` / `as E`, which type erasure turns
     * into no check at all, so a value of the wrong type was handed back and blew up as a `ClassCastException`
     * somewhere in the caller. The remote implementation deserializes against `entityClass` and so never had
     * this problem, meaning the two tiers disagreed on the same call.
     *
     * A mismatch is treated as a miss rather than an error: it means the cache name is being shared by two
     * entity types, and under LOCAL_REMOTE the read then falls through to the authoritative remote tier. The
     * warning is what makes the misconfiguration visible.
     */
    private fun <E : Any> asEntity(cacheName: String, stored: Any, entityClass: KClass<E>): E? {
        if (entityClass.java.isInstance(stored)) {
            @Suppress("UNCHECKED_CAST")
            return stored as E
        }
        log.warn(
            "Hash cache [{0}] holds a {1} where a {2} was requested; treating it as a miss. The same cache name is " +
                "most likely being used for more than one entity type.",
            cacheName, stored::class.java.name, entityClass.java.name
        )
        return null
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
        // Serialize the whole remove-store-reindex sequence for this entity; see [writeLocks].
        writeLockFor(cacheName, idStr).withLock {
            removeFromIndexes(cacheName, idStr)
            val cache = main(cacheName)
            cache.put(idStr, entity)
            filterableProperties.forEach { prop ->
                getPropertyValue(entity, prop)?.let { value ->
                    addToSetIndex(cacheName, prop, value.toString(), idStr)
                }
            }
            sortableProperties.forEach { prop ->
                getPropertyValue(entity, prop)?.let { value ->
                    addToZSetIndex(cacheName, prop, idStr, toDouble(value))
                }
            }
            cache.cleanUp()
        }
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
        // Same lock as [save]: otherwise a delete can interleave with a concurrent save of the same entity and
        // leave the main entry gone but its index entries behind (or vice versa).
        writeLockFor(cacheName, idStr).withLock {
            val cache = mainData[cacheName] ?: return
            cache.invalidate(idStr)
            cache.cleanUp()
            removeFromIndexes(cacheName, idStr)
        }
    }

    override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> {
        if (ids.isEmpty()) return emptyList()
        val cache = main(cacheName)
        return ids.mapNotNull { id ->
            cache.getIfPresent(normalizePkField(id))?.let { asEntity(cacheName, it, entityClass) }
        }
    }

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> =
        main(cacheName).asMap().values.mapNotNull { asEntity(cacheName, it, entityClass) }

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

    /**
     * 在内存里判定单个 entity 是否满足 [Criteria]——本地缓存没有 SQL where 引擎，只能 in-memory eval。
     *
     * Criteria 组之间是 AND 关系（任一组不匹配即整体 false）；组内类型：
     * - 单条 [Criterion]：调 [matchesCriterion]
     * - Array：OR 组，任一元素匹配即视为该组通过
     * - 嵌套 [Criteria]：递归
     *
     * @param entity 待匹配实体
     * @param criteria 查询条件
     * @return 是否匹配
     * @author K
     * @since 1.0.0
     */
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

    /**
     * 在内存里判定 entity 是否满足单个 [Criterion]。
     *
     * 操作符分流：
     * - EQ / IN：toString 比对（兼容字符串化 id —— 缓存里的 `Long 1` 与条件里的 `String "1"` 视为相等）
     * - GT/GE/LT/LE：toDouble 比对（数值字段）
     * - 其余：委托 [OperatorEnum.compare]
     *
     * 兜底分支原先是 `actual.toString() == expected.toString()`，即**退化成相等判定**。
     * 而 [OperatorEnum] 里恰好有 `NE("!=")` / `LG("<>")` / `NOT_IN` / `NOT_BETWEEN` 这些否定操作符，
     * 它们落到该分支后返回的结果与语义完全相反——查"不等于 X"反而只返回等于 X 的记录，
     * 且不抛异常、无日志。LIKE 族同样被静默降级成全等匹配。
     * 委托给 [OperatorEnum.compare] 后这些操作符都有了正确实现，也不必在此重复维护一套。
     *
     * null 处理：actual 或 expected 任一为 null 时返回 `c.operator.acceptNull`，
     * 与 SQL IS NULL / IS NOT NULL 语义对齐。
     *
     * @param entity 待匹配实体
     * @param c 单个条件
     * @return 是否匹配
     * @author K
     * @since 1.0.0
     */
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
            else -> c.operator.compare(actual, expected)
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

    private val log = LogFactory.getLog(this::class)

    companion object {
        const val DEFAULT_MAXIMUM_SIZE = 10_000L

        /**
         * Number of stripes guarding entity writes. 64 keeps contention negligible for the concurrency a local
         * cache sees while costing a fixed handful of objects; see [writeLocks].
         */
        private const val WRITE_LOCK_STRIPES = 64
    }
}
