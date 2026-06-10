package io.kudos.ability.data.memdb.redis.dao

import com.alibaba.fastjson2.JSON
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.consts.CacheKey
import io.kudos.ability.data.memdb.redis.dao.support.CriteriaRedisResolver
import io.kudos.base.logger.LogFactory
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.model.contract.entity.IIdEntity
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.serializer.RedisSerializer
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass

/**
 * Redis access object for entities with an id stored in a Hash structure.
 *
 * 1. Full-table cache (one object per row).
 * 2. Supports querying by multiple properties (id/type/active/time).
 * 3. Supports sorting.
 * 4. Supports pagination.
 * 5. Supports update/delete.
 *
 * Implementation:
 * 1. Main data uses a Hash (id → object JSON).
 * 2. Queries use secondary indexes (Set/ZSet); composite indexes use ZSet.
 * 3. Paged queries: ZREVRANGE or ZREVRANGEBYSCORE LIMIT + HMGET.
 * 4. Indexes are maintained on update/delete.
 * 5. Full-table refresh: pipeline batch write + tmp key + rename (to avoid a half-refreshed state).
 * 6. Uses pipeline (Lettuce async batch) to improve write performance.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class IdEntitiesRedisHashDao(
    protected val redisTemplates: RedisTemplates
) {

    /**
     * Canonical form of the primary key when used as a Hash field / Set or ZSet member, to avoid mismatches caused by
     * trailing whitespace in CHAR-like types versus the caller's trimmed primary key.
     */
    protected open fun normalizePkField(id: Any?): String = (id ?: "").toString().trim()

    // ---------- Single-row CRUD ----------

    /**
     * Saves or updates a single row, and maintains the secondary indexes (built from values taken from the entity using the supplied property sets).
     *
     * @param filterableProperties Filterable property names (use Set index for equality queries) such as type, status; not built if empty. Note: numeric range query conditions should be placed in sortableProperties.
     * @param sortableProperties Sortable / range property names (ZSet index) such as time, score; not built if empty. Numeric range query conditions go here.
     */
    open fun <PK, E : IIdEntity<PK>> save(
        dataKeyPrefix: String,
        entity: E,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    ) {
        val id = entity.id ?: throw IllegalArgumentException("entity.id must not be null")
        getRedisTemplate().opsForHash<String, Any>().put(dataKeyPrefix, normalizePkField(id), entity)
        updateIndexForEntity(dataKeyPrefix, id, entity, filterableProperties, sortableProperties, add = true)
    }

    /**
     * Batch save or update multiple rows and maintain the secondary indexes (built from values taken from the entities using the supplied property sets).
     * Main data is written in batches via pipeline to reduce network round-trips.
     *
     * @param filterableProperties Filterable property names (use Set index for equality queries); not built if empty. Numeric range query conditions go in sortableProperties.
     * @param sortableProperties Sortable / range property names (ZSet index); not built if empty. Numeric range query conditions go here.
     */
    open fun <PK, E : IIdEntity<PK>> saveBatch(
        dataKeyPrefix: String,
        entities: List<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    ) {
        val valid = entities.filter { e ->
            val id = e.id ?: return@filter false
            normalizePkField(id).isNotBlank()
        }
        if (valid.isEmpty()) return
        val template = getRedisTemplate()
        val hashKeySer = template.hashKeySerializer as? RedisSerializer<Any>
        val hashValSer = template.hashValueSerializer as? RedisSerializer<Any>
        val dataKeyBytes = dataKeyPrefix.toByteArray(StandardCharsets.UTF_8)
        template.executePipelined(RedisCallback<Any?> { connection ->
            valid.forEach { entity ->
                val id = entity.id
                val fieldBytes = hashKeySer?.serialize(normalizePkField(id))
                val valueBytes = hashValSer?.serialize(entity)
                if (fieldBytes != null && valueBytes != null) {
                    connection.hashCommands().hSet(dataKeyBytes, fieldBytes, valueBytes)
                }
            }
            null
        })
        valid.forEach { entity ->
            val id = entity.id
            updateIndexForEntity(dataKeyPrefix, id, entity, filterableProperties, sortableProperties, add = true)
        }
    }

    /**
     * Query a single row by id.
     */
    open fun <PK, E : IIdEntity<PK>> getById(dataKeyPrefix: String, id: PK, entityClass: KClass<E>): E? {
        val raw = getRedisTemplate().opsForHash<String, Any>().get(dataKeyPrefix, normalizePkField(id)) ?: return null
        return parseToEntity(raw, entityClass)
    }

    /**
     * Lightweight check for whether the specified id exists in the Hash (uses HEXISTS, does not deserialize the value).
     */
    open fun existsById(dataKeyPrefix: String, id: Any): Boolean =
        getRedisTemplate().opsForHash<String, Any>().hasKey(dataKeyPrefix, normalizePkField(id))

    /**
     * Delete a single row by id and remove it from the secondary indexes.
     *
     * @param filterableProperties The set of property names used when building the Set index; must match the one used at save time so removal from the index works correctly.
     * @param sortableProperties The set of property names used when building the ZSet index; must match the one used at save time so removal from the index works correctly.
     */
    open fun <PK, E : IIdEntity<PK>> deleteById(
        dataKeyPrefix: String,
        id: PK,
        entityClass: KClass<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    ) {
        val entity = getById(dataKeyPrefix, id, entityClass) ?: run {
            getRedisTemplate().opsForHash<String, Any>().delete(dataKeyPrefix, normalizePkField(id))
            return
        }
        getRedisTemplate().opsForHash<String, Any>().delete(dataKeyPrefix, normalizePkField(id))
        updateIndexForEntity(dataKeyPrefix, id, entity, filterableProperties, sortableProperties, add = false)
    }

    /**
     * Batch query by a list of ids.
     */
    open fun <E : IIdEntity<*>> findByIds(
        dataKeyPrefix: String,
        ids: Collection<*>,
        entityClass: KClass<E>
    ): List<E> {
        if (ids.isEmpty()) return emptyList()
        val fields = ids.map { normalizePkField(it) }
        val rawList = getRedisTemplate().opsForHash<String, Any>().multiGet(dataKeyPrefix, fields)
            ?: return emptyList()
        return rawList.mapNotNull { raw -> raw?.let { parseToEntity(it, entityClass) } }
    }

    // ---------- Full table and pagination ----------

    /**
     * Full-table list (without pagination).
     */
    open fun <PK, E : IIdEntity<PK>> listAll(dataKeyPrefix: String, entityClass: KClass<E>): List<E> {
        val entries = getRedisTemplate().opsForHash<String, Any>().entries(dataKeyPrefix) ?: return emptyList()
        return entries.values.mapNotNull { parseToEntity(it, entityClass) }
    }

    /**
     * Paged query by a ZSet secondary index (commonly used for paging sorted by time, etc.).
     * @param zsetIndexName Index name, corresponding to the xxx part of "zset:xxx" in the secondary index key.
     * @param offset Offset.
     * @param limit Count.
     * @param desc Whether to sort descending (default true, i.e. newest/largest first).
     */
    open fun <PK, E : IIdEntity<PK>> listPageByZSetIndex(
        dataKeyPrefix: String,
        entityClass: KClass<E>,
        zsetIndexName: String,
        offset: Long,
        limit: Long,
        desc: Boolean = true
    ): List<E> {
        val indexKey = CacheKey.getCacheKey(getIndexKeyPrefix(dataKeyPrefix), "zset", zsetIndexName)
        val ids = (
            if (desc)
                getRedisTemplate().opsForZSet().reverseRange(indexKey, offset, offset + limit - 1)
            else
                getRedisTemplate().opsForZSet().range(indexKey, offset, offset + limit - 1)
            ) ?: return emptyList()
        return findByIds(dataKeyPrefix, ids.filterNotNull(), entityClass)
    }

    /**
     * Equality query by a single property: collect all ids for the property, then fetch the rows (without pagination).
     * Uses the Set secondary index (set:property:value) uniformly, consistent with the writes performed for filterableProperties in save/saveBatch;
     * ZSet is used only for listPageByZSetIndex / sorting, not here.
     *
     * @param property Property name, corresponding to the property part of the secondary index key (e.g. "type", "status").
     * @param value Property value, converted to a string and used as part of the Set key.
     */
    open fun <PK, E : IIdEntity<PK>> listBySetIndex(
        dataKeyPrefix: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E> {
        val idxPrefix = getIndexKeyPrefix(dataKeyPrefix)
        val setKey = CacheKey.getCacheKey(idxPrefix, "set", property, value.toString())
        val ids = getRedisTemplate().opsForSet().members(setKey)?.mapNotNull { it.toString() }?.toList() ?: emptyList()
        return findByIds(dataKeyPrefix, ids, entityClass)
    }

    /**
     * Converts an arbitrary value to a Double for use as a ZSet score.
     * Non-numeric strings fall back to `-Double.MAX_VALUE` — not `Double.MIN_VALUE`, which is the smallest **positive** number.
     *
     * @param value Any value.
     * @return Double score.
     * @author K
     * @since 1.0.0
     */
    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        // -Double.MAX_VALUE is the most negative value; Double.MIN_VALUE is actually the smallest **positive** number and must never be used as the lower-bound fallback.
        is String -> value.toDoubleOrNull() ?: -Double.MAX_VALUE
        else -> -Double.MAX_VALUE
    }

    /**
     * Query a list by criteria + pagination + sorting.
     * - In Criteria, numeric property values use the ZSet index; others use the Set index; condition logic is AND between groups and OR within an in-group array.
     * - All properties in orders use the ZSet index; only the first Order participates in Redis sorting, multi-field sorting must be handled at the application layer.
     *
     * @param dataKeyPrefix Table key prefix.
     * @param entityClass Row object type.
     * @param criteria Query conditions; null means the entire table.
     * @param pageNo Page number, starting at 1.
     * @param pageSize Page size.
     * @param orders Sort (ZSet-indexed property); if omitted, pages in the natural order of the condition result set.
     */
    open fun <PK, E : IIdEntity<PK>> list(
        dataKeyPrefix: String,
        entityClass: KClass<E>,
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        vararg orders: Order
    ): List<E> {
        val template = getRedisTemplate()
        val indexKeyPrefix = getIndexKeyPrefix(dataKeyPrefix)
        val resolver = CriteriaRedisResolver(indexKeyPrefix, template)
        val ids: Set<String> = resolver.resolveToIds(criteria)
            ?: (template.opsForHash<String, Any>().keys(dataKeyPrefix) ?: emptySet())
        if (ids.isEmpty()) return emptyList()
        val pNo = if (pageNo < 1) 1 else pageNo
        val pSize = if (pageSize < 1) 1 else pageSize
        val offset = ((pNo - 1) * pSize).toLong()
        val limit = pSize.toLong()
        val pageIds: Collection<*>
        if (orders.isNotEmpty()) {
            val firstOrder = orders.first()
            val zsetKey = CacheKey.getCacheKey(indexKeyPrefix, "zset", firstOrder.property)
            val desc = firstOrder.direction == DirectionEnum.DESC
            val allOrdered = if (desc)
                template.opsForZSet().reverseRangeWithScores(zsetKey, 0, -1)
            else
                template.opsForZSet().rangeWithScores(zsetKey, 0, -1)
            val orderedIds = (allOrdered ?: emptySet())
                .asSequence()
                .mapNotNull { it.value?.toString() }
                .filter { ids.contains(it) }
                .toList()
            pageIds = orderedIds.drop(offset.toInt()).take(limit.toInt())
        } else {
            pageIds = ids.toList().drop(offset.toInt()).take(limit.toInt())
        }
        return findByIds(dataKeyPrefix, pageIds, entityClass)
    }

    /**
     * Full-table refresh: atomically replace the main data first (tmp + rename), then rebuild the secondary indexes based on the supplied property sets.
     *
     * @param filterableProperties Filterable property names (use Set index for equality queries); not built if empty. Numeric range query conditions go in sortableProperties.
     * @param sortableProperties Sortable / range property names (ZSet index); not built if empty. Numeric range query conditions go here.
     */
    open fun <PK, E : IIdEntity<PK>> refreshAll(
        dataKeyPrefix: String,
        entities: List<E>,
        filterableProperties: Set<String> = emptySet(),
        sortableProperties: Set<String> = emptySet()
    ) {
        if (entities.isEmpty()) {
            clear(dataKeyPrefix)
            return
        }
        val dataKeyBytes = dataKeyPrefix.toByteArray(StandardCharsets.UTF_8)
        val tmpKey = CacheKey.getCacheKey(dataKeyPrefix, "tmp", System.currentTimeMillis().toString())
        val tmpKeyBytes = tmpKey.toByteArray(StandardCharsets.UTF_8)
        val template = getRedisTemplate()
        val hashKeySer = template.hashKeySerializer as? RedisSerializer<Any>
        val hashValSer = template.hashValueSerializer as? RedisSerializer<Any>
        template.executePipelined(RedisCallback<Any?> { connection ->
            entities.forEach { entity ->
                val id = entity.id ?: return@forEach
                val fieldBytes = hashKeySer?.serialize(normalizePkField(id))
                val valueBytes = hashValSer?.serialize(entity)
                if (fieldBytes != null && valueBytes != null) {
                    connection.hashCommands().hSet(tmpKeyBytes, fieldBytes, valueBytes)
                }
            }
            null
        })
        template.execute { connection ->
            connection.keyCommands().rename(tmpKeyBytes, dataKeyBytes)
            null
        }
        deleteAllIndexKeys(dataKeyPrefix)
        entities.forEach { save(dataKeyPrefix, it, filterableProperties, sortableProperties) }
    }

    /**
     * Clear all data of this Hash (main data and secondary indexes).
     */
    open fun clear(dataKeyPrefix: String) {
        val dataKeyBytes = dataKeyPrefix.toByteArray(StandardCharsets.UTF_8)
        getRedisTemplate().execute { connection ->
            connection.keyCommands().del(dataKeyBytes)
            null
        }
        deleteAllIndexKeys(dataKeyPrefix)
    }

    /**
     * Delete all secondary index keys related to the current table (matched by prefix).
     * Uses SCAN instead of KEYS to be compatible with Lettuce 7.x (the KEYS API changed in Lettuce 7) and to avoid blocking.
     */
    protected open fun deleteAllIndexKeys(dataKeyPrefix: String) {
        val pattern = "${getIndexKeyPrefix(dataKeyPrefix)}*"
        getRedisTemplate().execute { connection ->
            val keyCmds = connection.keyCommands()
            keyCmds.scan(ScanOptions.scanOptions().match(pattern).build()).use { cursor ->
                while (cursor.hasNext()) {
                    keyCmds.del(cursor.next())
                }
            }
            null
        }
    }

    // ---------- Internal ----------

    /**
     * Incrementally maintains the entity's secondary indexes (Set + ZSet).
     *
     * - filterableProperties → Set index (equality queries)
     * - sortableProperties → ZSet index (sort / range queries)
     * - add=true writes to the index; add=false removes from the index
     *
     * Both index types are written "per property + per type" for the same entity, so write performance is proportional to the size of the property set.
     *
     * @param dataKeyPrefix Main data key prefix (used to derive the index key prefix).
     * @param id Entity primary key.
     * @param entity Entity object.
     * @param filterableProperties Property set participating in equality queries.
     * @param sortableProperties Property set participating in sort / range queries.
     * @param add true writes to the index, false removes from the index.
     * @author K
     * @since 1.0.0
     */
    private fun <PK, E : IIdEntity<PK>> updateIndexForEntity(
        dataKeyPrefix: String,
        id: PK,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>,
        add: Boolean
    ) {
        val idxPrefix = getIndexKeyPrefix(dataKeyPrefix)
        val idStr = normalizePkField(id)
        for (prop in filterableProperties) {
            getPropertyValue(entity, prop)?.let { value ->
                val key = CacheKey.getCacheKey(idxPrefix, "set", prop, value.toString())
                if (add) getRedisTemplate().opsForSet().add(key, idStr)
                else getRedisTemplate().opsForSet().remove(key, idStr)
            }
        }
        for (prop in sortableProperties) {
            getPropertyValue(entity, prop)?.let { value ->
                val score = toDouble(value)
                val key = CacheKey.getCacheKey(idxPrefix, "zset", prop)
                if (add) getRedisTemplate().opsForZSet().add(key, idStr, score)
                else getRedisTemplate().opsForZSet().remove(key, idStr)
            }
        }
    }

    /**
     * Reflectively fetches the value of the specified property on the entity.
     * Order: field → `getXxx` → `isXxx`; returns null if none is found.
     *
     * TODO Consider replacing per-call reflection with a property metadata cache.
     *
     * @param entity Target object.
     * @param propertyName Property name.
     * @return Property value, or null if not found.
     * @author K
     * @since 1.0.0
     */
    //TODO Performance improvement
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

    /**
     * Converts the raw value fetched from Redis (which may have already been deserialized to an object by Spring Data Redis,
     * or may still be a JSON string) uniformly into the target entity. Returns null and logs a warn on parse failure,
     * to prevent a single bad row from dragging down the entire list query.
     */
    private fun <E : IIdEntity<*>> parseToEntity(raw: Any, entityClass: KClass<E>): E? = try {
        when (raw) {
            is String -> JSON.parseObject(raw, entityClass.java)
            else -> JSON.parseObject(JSON.toJSONString(raw), entityClass.java)
        }
    } catch (e: Exception) {
        log.warn("Failed to deserialize redis hash value; skipping this record: type={0}, error={1}", entityClass.simpleName, e.message)
        null
    }

    /**
     * Returns the default RedisTemplate; subclasses can override this method to return a different instance when custom serialization or library choice is needed.
     *
     * @return Default RedisTemplate.
     * @author K
     * @since 1.0.0
     */
    protected fun getRedisTemplate(): RedisTemplate<Any, Any?> = redisTemplates.defaultRedisTemplate

    /** Secondary index key prefix. */
    protected fun getIndexKeyPrefix(dataKeyPrefix: String): String =
        CacheKey.getCacheKey(dataKeyPrefix, "idx")

    companion object {
        /** Logger; deserialization failures are only logged as WARN and the row is skipped, to prevent a single bad record from dragging down the whole batch query. */
        private val log = LogFactory.getLog(IdEntitiesRedisHashDao::class)
    }
}