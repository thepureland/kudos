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
 * Hash 缓存的 Caffeine 本地实现，内存结构模拟 Redis Hash + Set + ZSet；
 * 同时实现 [IHashCacheSync] 供收到 Redis 通知后清理本地。
 *
 * 主数据用 Caffeine 承载，按 cacheName 分桶设置 [maximumSize]，驱逐、删除、覆盖写入时同步清理
 * Set/ZSet 二级索引，避免主数据已不存在但索引仍返回旧 id。
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

    /** 主数据：cacheName → Caffeine(id → entity)；模拟 Redis hash，并提供容量上限 */
    private val mainData = ConcurrentHashMap<String, Cache<String, Any>>()
    /** Set 二级索引：cacheName → (propertyKey → ids)；模拟 Redis set，用于按属性等值查询 */
    private val setIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    /** ZSet 二级索引：cacheName → (propertyKey → (id → score))；模拟 Redis zset，用于按属性排序/范围 */
    private val zsetIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableMap<String, Double>>>()

    /** 取/惰性创建主数据空间 */
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

    /** 取/惰性创建 Set 索引空间 */
    private fun setIdx(cacheName: String) = setIndex.getOrPut(cacheName) { ConcurrentHashMap() }
    /** 取/惰性创建 ZSet 索引空间 */
    private fun zsetIdx(cacheName: String) = zsetIndex.getOrPut(cacheName) { ConcurrentHashMap() }

    /** Set 索引的复合 key：避免不同属性间命名冲突 */
    private fun setKey(property: String, value: String) = "set:$property:$value"
    /** ZSet 索引的复合 key：同上，单层 zset 按属性独立 */
    private fun zsetKey(property: String) = "zset:$property"

    /**
     * 从所有二级索引中移除指定 id，并清理空索引桶。
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
     * 主键在 Hash field / 索引中的规范形式，避免 CHAR 等类型尾部空格与调用方传入的 trim 后主键不一致导致无法命中。
     */
    private fun normalizePkField(id: Any?): String = (id ?: "").toString().trim()

    /**
     * 把任意值转 Double 作为 zset 的 score。
     * 非数值字符串或其它类型回落到 `-Double.MAX_VALUE`——不是 `Double.MIN_VALUE`，那是最小正数会让负分排错位置。
     *
     * @param value 任意值
     * @return Double score
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        // -Double.MAX_VALUE 是负方向最远；Double.MIN_VALUE 实为最小**正**数，
        // 作为下界回退会让所有负 score 成员排在更前面（错误顺序）。
        is String -> value.toDoubleOrNull() ?: -Double.MAX_VALUE
        else -> -Double.MAX_VALUE
    }

    /**
     * 反射取实体上指定属性的值。
     * 顺序：字段 (setAccessible) → `getXxx` → `isXxx`；都没有返回 null。
     * 失败仅 try-catch 吃掉异常，调用方对 null 已有降级。
     *
     * @param entity 目标对象
     * @param propertyName 属性名
     * @return 属性值；查不到返回 null
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
            // 同 toDouble：用 -Double.MAX_VALUE 作下界回退，避免负值被排错位置
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
