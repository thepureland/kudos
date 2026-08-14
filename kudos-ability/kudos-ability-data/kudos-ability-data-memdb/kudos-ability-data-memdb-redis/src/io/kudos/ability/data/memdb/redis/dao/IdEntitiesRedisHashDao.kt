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
import org.springframework.data.redis.connection.zset.Aggregate
import org.springframework.data.redis.connection.zset.Weights
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.core.ScanOptions
import org.springframework.data.redis.serializer.RedisSerializer
import java.lang.reflect.Field
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import java.time.Duration
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicLong
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
 * 4. Indexes are maintained on update/delete — including removal of entries for the *previous* values of an
 *    updated entity, so a changed property never leaves phantom ids behind in the old Set index.
 * 5. Full-table refresh: pipeline batch write of main data AND indexes into tmp keys, then per-key RENAME
 *    (atomic overwrite), so queries never observe a half-refreshed or index-less state.
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
     * When updating, index entries derived from the entity's previous property values are removed first, so a
     * changed value never leaves a stale id behind in the old index.
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
        val old = readOldEntityForIndexMaintenance(dataKeyPrefix, id, entity, filterableProperties, sortableProperties)
        getRedisTemplate().opsForHash<String, Any>().put(dataKeyPrefix, normalizePkField(id), entity)
        removeStaleIndexEntries(dataKeyPrefix, id, old, entity, filterableProperties, sortableProperties)
        updateIndexForEntity(dataKeyPrefix, id, entity, filterableProperties, sortableProperties, add = true)
    }

    /**
     * Batch save or update multiple rows and maintain the secondary indexes (built from values taken from the entities using the supplied property sets).
     * Main data is written in batches via pipeline to reduce network round-trips. Previous entity versions are
     * fetched (HMGET) before the write so stale index entries of changed values can be removed.
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
        val hasIndexes = filterableProperties.isNotEmpty() || sortableProperties.isNotEmpty()
        val fields = valid.map { normalizePkField(it.id) }
        // read previous versions before overwriting, keyed by pk field, for stale-index removal
        val oldByField: Map<String, Any?> = if (hasIndexes) {
            val rawList = template.opsForHash<String, Any>().multiGet(dataKeyPrefix, fields)
            fields.mapIndexed { i, field ->
                field to rawList?.getOrNull(i)?.let { parseToObject(it, valid[i].javaClass) }
            }.toMap()
        } else emptyMap()

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
            removeStaleIndexEntries(
                dataKeyPrefix, id, oldByField[normalizePkField(id)], entity, filterableProperties, sortableProperties
            )
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
     * Converts an arbitrary value to a Double for use as a ZSet score, or null when the value is not numeric.
     * Non-numeric values are NOT silently mapped to a sentinel score — writing a garbage score would corrupt
     * the sort order without any signal; callers skip the index entry and log instead.
     *
     * @param value Any value.
     * @return Double score, or null for non-numeric values.
     * @author K
     * @since 1.0.0
     */
    private fun toDoubleOrNull(value: Any): Double? = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull()
        else -> null
    }

    /**
     * Query a list by criteria + pagination + sorting.
     * - In Criteria, numeric property values use the ZSet index; others use the Set index; condition logic is AND between groups and OR within an in-group array.
     * - All properties in orders use the ZSet index; only the first Order participates in Redis sorting, multi-field sorting must be handled at the application layer.
     * - Without orders, ids are sorted lexicographically before paging so the same page number returns the same
     *   rows across calls (HKEYS / SMEMBERS order is unspecified and must not be paged over directly).
     *
     * Sorting and paging happen inside Redis rather than in the JVM:
     *  - no criteria → `ZRANGE`/`ZREVRANGE` the sort index directly for exactly one page;
     *  - with criteria → the candidate ids are staged in a temporary Set and `ZINTERSTORE`d with the sort index
     *    (weight 0 on the candidates, so the score comes only from the sort index), then one page is read back.
     * Either way the transferred data is proportional to the page, not to the table.
     *
     * Cluster note: the intersection touches several keys at once, so in Redis Cluster the index keys must hash
     * to one slot — give [dataKeyPrefix] a hash tag (e.g. `{myTable}:rows`) when deploying on a cluster.
     * The same requirement already applies to the `IN` unions in [CriteriaRedisResolver].
     *
     * @param dataKeyPrefix Table key prefix.
     * @param entityClass Row object type.
     * @param criteria Query conditions; null means the entire table.
     * @param pageNo Page number, starting at 1.
     * @param pageSize Page size.
     * @param orders Sort (ZSet-indexed property); if omitted, pages over the lexicographically sorted ids of the condition result set.
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
        // null means "no constraint"; distinguished from an empty result so the unfiltered path can page the
        // sort index directly instead of pulling every hash key back
        val criteriaIds: Set<String>? = resolver.resolveToIds(criteria)
        if (criteriaIds != null && criteriaIds.isEmpty()) return emptyList()
        val pNo = if (pageNo < 1) 1 else pageNo
        val pSize = if (pageSize < 1) 1 else pageSize
        val offset = ((pNo - 1) * pSize).toLong()
        val limit = pSize.toLong()
        val pageIds: Collection<*> = if (orders.isNotEmpty()) {
            val firstOrder = orders.first()
            val zsetKey = CacheKey.getCacheKey(indexKeyPrefix, "zset", firstOrder.property)
            val desc = firstOrder.direction == DirectionEnum.DESC
            if (criteriaIds == null) {
                rangeIds(zsetKey, desc, offset, offset + limit - 1)
            } else {
                pageIdsByIndexIntersection(indexKeyPrefix, zsetKey, criteriaIds, desc, offset, limit)
            }
        } else {
            val ids = criteriaIds ?: (template.opsForHash<String, Any>().keys(dataKeyPrefix) ?: emptySet())
            if (ids.isEmpty()) return emptyList()
            ids.sorted().drop(offset.toInt()).take(limit.toInt())
        }
        if (pageIds.isEmpty()) return emptyList()
        return findByIds(dataKeyPrefix, pageIds, entityClass)
    }

    /** Reads one page of members from a ZSet index, honouring the sort direction. */
    private fun rangeIds(zsetKey: String, desc: Boolean, start: Long, end: Long): List<String> {
        val zsetOps = getRedisTemplate().opsForZSet()
        val ids = if (desc) zsetOps.reverseRange(zsetKey, start, end) else zsetOps.range(zsetKey, start, end)
        return ids?.mapNotNull { it?.toString() } ?: emptyList()
    }

    /**
     * Sorts and pages the candidate ids inside Redis: stage them in a temporary Set, `ZINTERSTORE` that Set with
     * the sort index into a temporary ZSet, then read exactly one page from it.
     *
     * The weights are `(1, 0)`: the sort index contributes its score, the candidate Set contributes 0 (a Set
     * member counts as score 1 in ZINTERSTORE, which would otherwise shift every score by one). Temporary keys
     * are always deleted, including on failure.
     */
    private fun pageIdsByIndexIntersection(
        indexKeyPrefix: String,
        zsetKey: String,
        candidateIds: Set<String>,
        desc: Boolean,
        offset: Long,
        limit: Long
    ): List<String> {
        val template = getRedisTemplate()
        val suffix = nextTmpSuffix()
        val tmpCandidateKey = CacheKey.getCacheKey(indexKeyPrefix, "tmp", "cand", suffix)
        val tmpSortedKey = CacheKey.getCacheKey(indexKeyPrefix, "tmp", "sorted", suffix)
        try {
            template.opsForSet().add(tmpCandidateKey, *candidateIds.toTypedArray<Any?>())
            template.opsForZSet().intersectAndStore(
                zsetKey, listOf(tmpCandidateKey), tmpSortedKey, Aggregate.SUM, Weights.of(1, 0)
            )
            return rangeIds(tmpSortedKey, desc, offset, offset + limit - 1)
        } finally {
            template.delete(listOf<Any>(tmpCandidateKey, tmpSortedKey))
        }
    }

    /**
     * Full-table refresh with per-key atomic swap: main data AND the secondary indexes are first written to
     * temporary keys via pipeline, then each temporary key is RENAMEd over its final key (RENAME atomically
     * replaces the destination), and finally index keys that the new data set no longer produces are deleted.
     *
     * Unlike a delete-then-rebuild, queries during a refresh always see either the complete old index or the
     * complete new one for any given key; there is no window with empty indexes. (The swap is atomic per key,
     * not across keys — a query spanning multiple index keys can briefly observe a mix of old and new.)
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
        val valid = entities.filter { normalizePkField(it.id).isNotBlank() }
        if (valid.isEmpty()) {
            clear(dataKeyPrefix)
            return
        }
        val template = getRedisTemplate()
        val idxPrefix = getIndexKeyPrefix(dataKeyPrefix)

        // aggregate all index entries in memory: final key -> members (set) / member scores (zset)
        val setIndex = mutableMapOf<String, MutableSet<String>>()
        val zsetIndex = mutableMapOf<String, MutableMap<String, Double>>()
        for (entity in valid) {
            val idStr = normalizePkField(entity.id)
            for (prop in filterableProperties) {
                getPropertyValue(entity, prop)?.let { value ->
                    val key = CacheKey.getCacheKey(idxPrefix, "set", prop, value.toString())
                    setIndex.getOrPut(key) { mutableSetOf() }.add(idStr)
                }
            }
            for (prop in sortableProperties) {
                getPropertyValue(entity, prop)?.let { value ->
                    val score = toDoubleOrNull(value)
                    if (score == null) {
                        log.warn(
                            "Non-numeric value for sortable property; skipping ZSet index entry: property={0}, id={1}",
                            prop, idStr
                        )
                    } else {
                        val key = CacheKey.getCacheKey(idxPrefix, "zset", prop)
                        zsetIndex.getOrPut(key) { mutableMapOf() }[idStr] = score
                    }
                }
            }
        }

        // index keys existing before this refresh; the ones this round does not overwrite are deleted at the end
        val oldIndexKeys = scanIndexKeys(dataKeyPrefix)

        val tmpSuffix = CacheKey.getCacheKey("tmp", nextTmpSuffix())
        val tmpKeyOf = { finalKey: String -> CacheKey.getCacheKey(finalKey, tmpSuffix) }
        val tmpDataKey = tmpKeyOf(dataKeyPrefix)

        val hashKeySer = template.hashKeySerializer as? RedisSerializer<Any>
        val hashValSer = template.hashValueSerializer as? RedisSerializer<Any>
        // set / zset members are written at connection level but must match what opsForSet/opsForZSet would
        // write (and what the readers deserialize), i.e. the template's value serializer
        val valueSer = template.valueSerializer as? RedisSerializer<Any>

        template.executePipelined(RedisCallback<Any?> { connection ->
            val tmpDataKeyBytes = tmpDataKey.toByteArray(StandardCharsets.UTF_8)
            valid.forEach { entity ->
                val fieldBytes = hashKeySer?.serialize(normalizePkField(entity.id))
                val valueBytes = hashValSer?.serialize(entity)
                if (fieldBytes != null && valueBytes != null) {
                    connection.hashCommands().hSet(tmpDataKeyBytes, fieldBytes, valueBytes)
                }
            }
            setIndex.forEach { (finalKey, members) ->
                val tmpKeyBytes = tmpKeyOf(finalKey).toByteArray(StandardCharsets.UTF_8)
                val memberBytes = members.mapNotNull { valueSer?.serialize(it) }.toTypedArray()
                if (memberBytes.isNotEmpty()) {
                    connection.setCommands().sAdd(tmpKeyBytes, *memberBytes)
                }
            }
            zsetIndex.forEach { (finalKey, memberScores) ->
                val tmpKeyBytes = tmpKeyOf(finalKey).toByteArray(StandardCharsets.UTF_8)
                memberScores.forEach { (member, score) ->
                    valueSer?.serialize(member)?.let { memberBytes ->
                        connection.zSetCommands().zAdd(tmpKeyBytes, score, memberBytes)
                    }
                }
            }
            null
        })

        // per-key atomic swap: RENAME overwrites the destination
        template.execute { connection ->
            val keyCmds = connection.keyCommands()
            keyCmds.rename(tmpDataKey.toByteArray(StandardCharsets.UTF_8), dataKeyPrefix.toByteArray(StandardCharsets.UTF_8))
            (setIndex.keys.asSequence() + zsetIndex.keys.asSequence()).forEach { finalKey ->
                keyCmds.rename(
                    tmpKeyOf(finalKey).toByteArray(StandardCharsets.UTF_8),
                    finalKey.toByteArray(StandardCharsets.UTF_8)
                )
            }
            null
        }

        // drop index keys the new data set no longer produces (e.g. set:type:A when no entity has type A anymore).
        // Transient keys are excluded: a concurrent paged query stages its candidates under the same index
        // prefix, and deleting those from under it would silently shorten its page.
        val staleKeys = oldIndexKeys.filterNot { it.contains(TMP_KEY_MARKER) } - setIndex.keys - zsetIndex.keys
        if (staleKeys.isNotEmpty()) {
            template.execute { connection ->
                staleKeys.forEach { connection.keyCommands().del(it.toByteArray(StandardCharsets.UTF_8)) }
                null
            }
        }
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
     * Applies [ttl] to **every** key this Hash owns under [dataKeyPrefix] — the main data key and all of its
     * secondary index keys.
     *
     * Exists because only this class knows the key layout. A caller that wanted to bound staleness could
     * otherwise only guess at it, and expiring the main key alone is actively harmful: the Set/ZSet indexes
     * would outlive it and keep handing out ids whose entities are gone, so `listBySetIndex` would return
     * phantom members. All keys therefore get the same TTL in one pass, which also keeps them aligned when this
     * is re-invoked after each write.
     *
     * Note that the expiry is not transactional across keys — Redis applies the commands one after another, so
     * a crash midway can leave a subset with the old TTL. The next write re-applies it, and the failure mode is
     * a shorter-than-configured lifetime rather than a partially expired region.
     *
     * Index keys are enumerated with SCAN (same reason as [deleteAllIndexKeys]: it avoids blocking and is
     * compatible with Lettuce 7.x), so the cost is proportional to the number of index keys for this prefix.
     * Callers with no TTL configured should simply not call this.
     *
     * @param dataKeyPrefix main data key prefix
     * @param ttl time-to-live to apply; must be positive
     * @author K
     * @since 1.0.0
     */
    open fun expireAll(dataKeyPrefix: String, ttl: Duration) {
        require(!ttl.isNegative && !ttl.isZero) { "ttl must be positive, got $ttl" }
        val template = getRedisTemplate()
        template.expire(dataKeyPrefix, ttl)
        val pattern = "${getIndexKeyPrefix(dataKeyPrefix)}*"
        val seconds = ttl.seconds
        template.execute { connection ->
            val keyCmds = connection.keyCommands()
            keyCmds.scan(ScanOptions.scanOptions().match(pattern).build()).use { cursor ->
                while (cursor.hasNext()) {
                    keyCmds.expire(cursor.next(), seconds)
                }
            }
            null
        }
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
     * Collects all existing secondary index key names of this table (SCAN by prefix).
     */
    private fun scanIndexKeys(dataKeyPrefix: String): Set<String> {
        val pattern = "${getIndexKeyPrefix(dataKeyPrefix)}*"
        val keys = mutableSetOf<String>()
        getRedisTemplate().execute { connection ->
            connection.keyCommands().scan(ScanOptions.scanOptions().match(pattern).build()).use { cursor ->
                while (cursor.hasNext()) {
                    keys.add(String(cursor.next(), StandardCharsets.UTF_8))
                }
            }
            null
        }
        return keys
    }

    /**
     * Reads the previous version of the entity (for index maintenance on update); returns null when no index
     * properties are configured — the read would be pure overhead — or when the row does not exist yet.
     */
    private fun readOldEntityForIndexMaintenance(
        dataKeyPrefix: String,
        id: Any?,
        entity: Any,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ): Any? {
        if (filterableProperties.isEmpty() && sortableProperties.isEmpty()) return null
        val raw = getRedisTemplate().opsForHash<String, Any>().get(dataKeyPrefix, normalizePkField(id)) ?: return null
        return parseToObject(raw, entity.javaClass)
    }

    /**
     * Removes index entries pointing at the entity's *previous* property values when those values changed.
     * Without this, updating e.g. `type` from A to B would leave the id in `idx:set:type:A` forever, and
     * equality queries on the old value would return phantom rows.
     *
     * ZSet indexes key on the property (member = id, score = value), so a changed numeric value is simply
     * overwritten by the subsequent add; removal is only needed when the new value no longer yields a score.
     */
    private fun removeStaleIndexEntries(
        dataKeyPrefix: String,
        id: Any?,
        old: Any?,
        entity: Any,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        if (old == null) return
        val idxPrefix = getIndexKeyPrefix(dataKeyPrefix)
        val idStr = normalizePkField(id)
        for (prop in filterableProperties) {
            val oldValue = getPropertyValue(old, prop) ?: continue
            val newValue = getPropertyValue(entity, prop)
            if (newValue == null || oldValue.toString() != newValue.toString()) {
                val key = CacheKey.getCacheKey(idxPrefix, "set", prop, oldValue.toString())
                getRedisTemplate().opsForSet().remove(key, idStr)
            }
        }
        for (prop in sortableProperties) {
            getPropertyValue(old, prop) ?: continue
            val newValue = getPropertyValue(entity, prop)
            if (newValue == null || toDoubleOrNull(newValue) == null) {
                val key = CacheKey.getCacheKey(idxPrefix, "zset", prop)
                getRedisTemplate().opsForZSet().remove(key, idStr)
            }
        }
    }

    /**
     * Incrementally maintains the entity's secondary indexes (Set + ZSet).
     *
     * - filterableProperties → Set index (equality queries)
     * - sortableProperties → ZSet index (sort / range queries)
     * - add=true writes to the index; add=false removes from the index
     *
     * Non-numeric values of sortable properties are skipped with a WARN instead of being written with a
     * sentinel score (which would silently corrupt the sort order).
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
                val key = CacheKey.getCacheKey(idxPrefix, "zset", prop)
                if (!add) {
                    getRedisTemplate().opsForZSet().remove(key, idStr)
                } else {
                    val score = toDoubleOrNull(value)
                    if (score == null) {
                        log.warn(
                            "Non-numeric value for sortable property; skipping ZSet index entry: property={0}, id={1}",
                            prop, idStr
                        )
                    } else {
                        getRedisTemplate().opsForZSet().add(key, idStr, score)
                    }
                }
            }
        }
    }

    /**
     * Fetches the value of the specified property on the entity via a cached accessor.
     * Resolution order (cached per class+property): field → `getXxx` → `isXxx`; returns null if none is found.
     *
     * @param entity Target object.
     * @param propertyName Property name.
     * @return Property value, or null if not found.
     * @author K
     * @since 1.0.0
     */
    private fun getPropertyValue(entity: Any, propertyName: String): Any? {
        val clazz = entity.javaClass
        val accessor = propertyAccessorCache.getOrPut("${clazz.name}#$propertyName") {
            resolvePropertyAccessor(clazz, propertyName)
        }
        return when (accessor) {
            is Field -> accessor.get(entity)
            is Method -> accessor.invoke(entity)
            else -> null
        }
    }

    /** Resolves the reflective accessor for the property, or [NO_ACCESSOR] when the class has none. */
    private fun resolvePropertyAccessor(clazz: Class<*>, propertyName: String): Any {
        try {
            val field = clazz.getDeclaredField(propertyName)
            field.isAccessible = true
            return field
        } catch (_: NoSuchFieldException) { }
        val getterName = "get" + propertyName.replaceFirstChar { it.uppercase() }
        try {
            return clazz.getMethod(getterName)
        } catch (_: NoSuchMethodException) { }
        val boolGetterName = "is" + propertyName.replaceFirstChar { it.uppercase() }
        try {
            return clazz.getMethod(boolGetterName)
        } catch (_: NoSuchMethodException) { }
        return NO_ACCESSOR
    }

    /**
     * Converts the raw value fetched from Redis uniformly into the target entity type.
     * Fast path: when the configured serializer already produced an instance of the target type (JDK or
     * class-name-aware JSON serializers), it is returned as-is without a JSON round-trip. Otherwise raw JSON
     * strings are parsed directly and any other shape is re-serialized through JSON as a last resort.
     * Returns null and logs a warn on parse failure, to prevent a single bad row from dragging down the entire list query.
     */
    private fun <E : IIdEntity<*>> parseToEntity(raw: Any, entityClass: KClass<E>): E? {
        @Suppress("UNCHECKED_CAST")
        return parseToObject(raw, entityClass.java) as E?
    }

    /** Untyped variant of [parseToEntity]; also used to revive the previous entity version for index maintenance. */
    private fun parseToObject(raw: Any, entityClass: Class<*>): Any? = try {
        when {
            entityClass.isInstance(raw) -> raw
            raw is String -> JSON.parseObject(raw, entityClass)
            else -> JSON.parseObject(JSON.toJSONString(raw), entityClass)
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

        /** Cache-miss marker for [propertyAccessorCache]. */
        private val NO_ACCESSOR = Any()

        /** Reflective accessor cache ("class#property" → Field / Method / [NO_ACCESSOR]); avoids per-call reflection lookups. */
        private val propertyAccessorCache = ConcurrentHashMap<String, Any>()

        /** Segment marking a key as transient (staging area of a refresh or of a paged query). */
        internal const val TMP_KEY_MARKER = ":tmp:"

        /** Monotonic part of temporary key names; a timestamp alone collides between concurrent callers. */
        private val tmpKeyCounter = AtomicLong()

        /**
         * Builds a suffix unique per JVM for temporary keys (`<millis>-<counter>`). The counter is what makes
         * concurrent refreshes / paged queries safe; the timestamp only keeps the keys readable when debugging.
         */
        private fun nextTmpSuffix(): String = "${System.currentTimeMillis()}-${tmpKeyCounter.incrementAndGet()}"
    }
}
