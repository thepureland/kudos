package io.kudos.ability.data.memdb.redis.dao

import com.alibaba.fastjson2.JSON
import io.kudos.ability.data.memdb.redis.RedisTemplates
import io.kudos.ability.data.memdb.redis.consts.CacheKey
import io.kudos.ability.data.memdb.redis.dao.support.CriteriaRedisResolver
import io.kudos.base.lang.math.NumberKit
import io.kudos.base.query.Criteria
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import org.springframework.data.redis.core.RedisCallback
import org.springframework.data.redis.core.RedisTemplate
import org.springframework.data.redis.serializer.RedisSerializer
import java.lang.reflect.Method
import java.nio.charset.StandardCharsets
import kotlin.reflect.KClass
import kotlin.reflect.full.declaredMemberProperties
import kotlin.reflect.full.memberProperties

/**
 * 以Hash结构存储的带有id的实体的redis访问对象
 *
 * 1.全表缓存（每行一个对象）
 * 2.支持按多个属性查询（id/type/active/time）
 * 3.支持排序
 * 4.支持分页
 * 5.支持更新/删除
 *
 * 实现方式：
 * 1.主数据用 Hash（id → 对象JSON）
 * 2.查询用二级索引（Set/ZSet），组合索引 ZSet
 * 3.分页查询：ZREVRANGE 或 ZREVRANGEBYSCORE LIMIT + HMGET
 * 4.更新/删除时维护索引
 * 5.全表刷新：pipeline 批量写 + tmp key + rename（避免半刷新）
 * 6.用 pipeline（Lettuce async 批量），提升写入性能
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
open class IdEntitiesRedisHashDao(
    protected val redisTemplates: RedisTemplates
) {

    // ---------- 单条 CRUD ----------

    /**
     * 保存或更新一行，并维护二级索引（根据传入的属性集合从实体取值建索引）。
     *
     * @param setIndexPropertyNames 使用 Set 索引的属性名（等值查询），如 type、status；空则不建 Set 索引
     * @param zsetIndexPropertyNames 使用 ZSet 索引的属性名（排序/范围），如 time、score；空则不建 ZSet 索引
     */
    open fun <PK, E : IIdEntity<PK>> save(
        dataKeyPrefix: String,
        entity: E,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    ) {
        val id = entity.id ?: throw IllegalArgumentException("entity.id must not be null")
        getRedisTemplate().opsForHash<String, Any>().put(dataKeyPrefix, id.toString(), entity)
        updateIndexForEntity(dataKeyPrefix, id, entity, setIndexPropertyNames, zsetIndexPropertyNames, add = true)
    }

    /**
     * 批量保存或更新多行，并维护二级索引（根据传入的属性集合从实体取值建索引）。
     * 主数据使用 pipeline 批量写入，减少网络往返。
     *
     * @param setIndexPropertyNames 使用 Set 索引的属性名；空则不建 Set 索引
     * @param zsetIndexPropertyNames 使用 ZSet 索引的属性名；空则不建 ZSet 索引
     */
    open fun <PK, E : IIdEntity<PK>> saveBatch(
        dataKeyPrefix: String,
        entities: List<E>,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    ) {
        if (entities.isEmpty()) return
        val template = getRedisTemplate()
        val hashKeySer = template.hashKeySerializer as? RedisSerializer<Any>
        val hashValSer = template.hashValueSerializer as? RedisSerializer<Any>
        val dataKeyBytes = dataKeyPrefix.toByteArray(StandardCharsets.UTF_8)
        template.executePipelined(RedisCallback<Any?> { connection ->
            entities.forEach { entity ->
                val id = entity.id ?: return@forEach
                val fieldBytes = hashKeySer?.serialize(id.toString())
                val valueBytes = hashValSer?.serialize(entity)
                if (fieldBytes != null && valueBytes != null) {
                    connection.hashCommands().hSet(dataKeyBytes, fieldBytes, valueBytes)
                }
            }
            null
        })
        entities.forEach { entity ->
            val id = entity.id ?: return@forEach
            updateIndexForEntity(dataKeyPrefix, id, entity, setIndexPropertyNames, zsetIndexPropertyNames, add = true)
        }
    }

    /**
     * 按 id 查询一行。
     */
    open fun <PK, E : IIdEntity<PK>> getById(dataKeyPrefix: String, id: PK, entityClass: KClass<E>): E? {
        val raw = getRedisTemplate().opsForHash<String, Any>().get(dataKeyPrefix, id.toString()) ?: return null
        return parseToEntity(raw, entityClass)
    }

    /**
     * 按 id 删除一行，并从二级索引中移除。
     *
     * @param setIndexPropertyNames 建 Set 索引时用的属性名集合，需与保存时一致才能正确从索引移除
     * @param zsetIndexPropertyNames 建 ZSet 索引时用的属性名集合，需与保存时一致才能正确从索引移除
     */
    open fun <PK, E : IIdEntity<PK>> deleteById(
        dataKeyPrefix: String,
        id: PK,
        entityClass: KClass<E>,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    ) {
        val entity = getById(dataKeyPrefix, id, entityClass) ?: run {
            getRedisTemplate().opsForHash<String, Any>().delete(dataKeyPrefix, id.toString())
            return
        }
        getRedisTemplate().opsForHash<String, Any>().delete(dataKeyPrefix, id.toString())
        updateIndexForEntity(dataKeyPrefix, id, entity, setIndexPropertyNames, zsetIndexPropertyNames, add = false)
    }

    /**
     * 按 id 列表批量查询。
     */
    open fun <E : IIdEntity<*>> findByIds(
        dataKeyPrefix: String,
        ids: Collection<*>,
        entityClass: KClass<E>
    ): List<E> {
        if (ids.isEmpty()) return emptyList()
        val fields = ids.map { it.toString() }
        val rawList = getRedisTemplate().opsForHash<String, Any>().multiGet(dataKeyPrefix, fields)
            ?: return emptyList()
        return rawList.mapNotNull { raw -> raw?.let { parseToEntity(it, entityClass) } }
    }

    // ---------- 全表与分页 ----------

    /**
     * 全表列表（不分页）。
     */
    open fun <PK, E : IIdEntity<PK>> listAll(dataKeyPrefix: String, entityClass: KClass<E>): List<E> {
        val entries = getRedisTemplate().opsForHash<String, Any>().entries(dataKeyPrefix) ?: return emptyList()
        return entries.values.mapNotNull { parseToEntity(it, entityClass) }
    }

    /**
     * 按某个 ZSet 二级索引分页查询（常用于按时间等排序分页）。
     * @param zsetIndexName 索引名，对应二级索引 key 中 "zset:xxx" 的 xxx 部分
     * @param offset 偏移
     * @param limit 条数
     * @param desc 是否倒序（默认 true，即最新/最大在前）
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
        val ids = if (desc)
            getRedisTemplate().opsForZSet().reverseRange(indexKey, offset, offset + limit - 1)
        else
            getRedisTemplate().opsForZSet().range(indexKey, offset, offset + limit - 1)
                ?: return emptyList()
        return findByIds(dataKeyPrefix, ids.filterNotNull(), entityClass)
    }

    /**
     * 按某个属性的等值条件查询：该属性下的所有 id，再查行（不分页）。
     * 根据 [value] 是否数值型自动选择索引：数值型用 ZSet（zset:property），非数值型用 Set（set:property:value）。
     *
     * @param property 属性名，对应二级索引 key 中的属性部分（如 "type"、"status"）
     * @param value 属性值；为数值型时走 ZSet 索引，否则走 Set 索引
     */
    open fun <PK, E : IIdEntity<PK>> listBySetIndex(
        dataKeyPrefix: String,
        entityClass: KClass<E>,
        property: String,
        value: Any
    ): List<E> {
        val idxPrefix = getIndexKeyPrefix(dataKeyPrefix)
        val ids = if (NumberKit.isNumber(value.toString())) {
            val v = toDouble(value)
            val zsetKey = CacheKey.getCacheKey(idxPrefix, "zset", property)
            getRedisTemplate().opsForZSet().rangeByScore(zsetKey, v, v)?.mapNotNull { it?.toString() }?.toList()
                ?: emptyList()
        } else {
            val setKey = CacheKey.getCacheKey(idxPrefix, "set", property, value.toString())
            getRedisTemplate().opsForSet().members(setKey)?.mapNotNull { it.toString() }?.toList() ?: emptyList()
        }
        return findByIds(dataKeyPrefix, ids, entityClass)
    }

    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: Double.MIN_VALUE
        else -> Double.MIN_VALUE
    }

    /**
     * 按条件 + 分页 + 排序查询列表。
     * - Criteria 中属性值为数值型的用 ZSet 索引，其他用 Set 索引；条件逻辑 组间 AND，组内数组 OR。
     * - orders 中的属性全部用 ZSet 索引；仅第一个 Order 参与 Redis 排序，多字段排序需在应用层处理。
     *
     * @param dataKeyPrefix 表 key 前缀
     * @param entityClass 行对象类型
     * @param criteria 查询条件，可为空表示全表
     * @param pageNo 页码，从 1 开始
     * @param pageSize 每页条数
     * @param orders 排序（ZSet 索引属性），如不传则按条件结果集顺序分页
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
        val ids: Set<String> = resolver.resolveToIds(criteria) ?: run {
            val keys = template.opsForHash<String, Any>().keys(dataKeyPrefix) ?: emptySet()
            keys.map { it }.toSet()
        }
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
     * 全表刷新：先原子替换主数据（tmp + rename），再按传入的属性集合重建二级索引。
     *
     * @param setIndexPropertyNames 使用 Set 索引的属性名；空则不建 Set 索引
     * @param zsetIndexPropertyNames 使用 ZSet 索引的属性名；空则不建 ZSet 索引
     */
    open fun <PK, E : IIdEntity<PK>> refreshAll(
        dataKeyPrefix: String,
        entities: List<E>,
        setIndexPropertyNames: Set<String> = emptySet(),
        zsetIndexPropertyNames: Set<String> = emptySet()
    ) {
        val dataKeyBytes = dataKeyPrefix.toByteArray(StandardCharsets.UTF_8)
        if (entities.isEmpty()) {
            getRedisTemplate().execute { connection ->
                connection.keyCommands().del(dataKeyBytes)
                null
            }
            deleteAllIndexKeys(dataKeyPrefix)
            return
        }
        val tmpKey = CacheKey.getCacheKey(dataKeyPrefix, "tmp", System.currentTimeMillis().toString())
        val tmpKeyBytes = tmpKey.toByteArray(StandardCharsets.UTF_8)
        val template = getRedisTemplate()
        val hashKeySer = template.hashKeySerializer as? RedisSerializer<Any>
        val hashValSer = template.hashValueSerializer as? RedisSerializer<Any>
        getRedisTemplate().executePipelined(RedisCallback<Any?> { connection ->
            entities.forEach { entity ->
                val id = entity.id ?: return@forEach
                val fieldBytes = hashKeySer?.serialize(id.toString())
                val valueBytes = hashValSer?.serialize(entity)
                if (fieldBytes != null && valueBytes != null) {
                    connection.hashCommands().hSet(tmpKeyBytes, fieldBytes, valueBytes)
                }
            }
            null
        })
        getRedisTemplate().execute { connection ->
            connection.keyCommands().rename(tmpKeyBytes, dataKeyBytes)
            null
        }
        deleteAllIndexKeys(dataKeyPrefix)
        entities.forEach { save(dataKeyPrefix, it, setIndexPropertyNames, zsetIndexPropertyNames) }
    }

    /**
     * 删除当前表相关的所有二级索引 key（按前缀匹配）。
     */
    protected open fun deleteAllIndexKeys(dataKeyPrefix: String) {
        val pattern = "${getIndexKeyPrefix(dataKeyPrefix)}*"
        getRedisTemplate().execute { connection ->
            val keys = connection.keyCommands().keys(pattern.toByteArray(StandardCharsets.UTF_8))
            keys?.forEach { connection.keyCommands().del(it) }
            null
        }
    }

    // ---------- 内部 ----------

    private fun <PK, E : IIdEntity<PK>> updateIndexForEntity(
        dataKeyPrefix: String,
        id: PK,
        entity: E,
        setIndexPropertyNames: Set<String>,
        zsetIndexPropertyNames: Set<String>,
        add: Boolean
    ) {
        val idxPrefix = getIndexKeyPrefix(dataKeyPrefix)
        val idStr = id.toString()
        for (prop in setIndexPropertyNames) {
            getPropertyValue(entity, prop)?.let { value ->
                val key = CacheKey.getCacheKey(idxPrefix, "set", prop, value.toString())
                if (add) getRedisTemplate().opsForSet().add(key, idStr)
                else getRedisTemplate().opsForSet().remove(key, idStr)
            }
        }
        for (prop in zsetIndexPropertyNames) {
            getPropertyValue(entity, prop)?.let { value ->
                val score = toDouble(value)
                val key = CacheKey.getCacheKey(idxPrefix, "zset", prop)
                if (add) getRedisTemplate().opsForZSet().add(key, idStr, score)
                else getRedisTemplate().opsForZSet().remove(key, idStr)
            }
        }
    }

    //TODO 性能提升
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

    @Suppress("UNCHECKED_CAST")
    private fun <E : IIdEntity<*>> parseToEntity(raw: Any, entityClass: KClass<E>): E? = try {
        when (raw) {
            is String -> JSON.parseObject(raw, entityClass.java)
            else -> JSON.parseObject(JSON.toJSONString(raw), entityClass.java)
        }
    } catch (_: Exception) {
        null
    }

    protected fun getRedisTemplate(): RedisTemplate<Any, Any?> {
        return redisTemplates.defaultRedisTemplate
    }

    /** 二级索引 key 前缀 */
    protected fun getIndexKeyPrefix(dataKeyPrefix: String): String {
        return CacheKey.getCacheKey(dataKeyPrefix, "idx")
    }
}