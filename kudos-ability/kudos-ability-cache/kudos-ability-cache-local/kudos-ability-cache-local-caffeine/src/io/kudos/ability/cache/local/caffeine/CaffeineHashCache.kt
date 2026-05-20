package io.kudos.ability.cache.local.caffeine

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
 * 已知限制（待后续单独迭代）：mainData / setIndex / zsetIndex 是裸的 [ConcurrentHashMap]，
 * 没有像 KV 端那样的 Caffeine maximumSize 兜底。同一 cacheName 下条目数受限于业务数据规模，
 * 跑长之后存在被业务无界化的风险（典型场景：用户量大且全量缓存）。
 * 若要彻底治理，需要把 [mainData] 改成 Caffeine LoadingCache 并同步驱逐两个索引；本轮范围之外。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class CaffeineHashCache : IHashCache, IHashCacheSync {

    /** 主数据：cacheName → (id → entity)；模拟 Redis hash */
    private val mainData = ConcurrentHashMap<String, ConcurrentHashMap<String, Any>>()
    /** Set 二级索引：cacheName → (propertyKey → ids)；模拟 Redis set，用于按属性等值查询 */
    private val setIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    /** ZSet 二级索引：cacheName → (propertyKey → (id → score))；模拟 Redis zset，用于按属性排序/范围 */
    private val zsetIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableMap<String, Double>>>()

    /** 取/惰性创建主数据空间 */
    private fun main(cacheName: String) = mainData.getOrPut(cacheName) { ConcurrentHashMap() }
    /** 取/惰性创建 Set 索引空间 */
    private fun setIdx(cacheName: String) = setIndex.getOrPut(cacheName) { ConcurrentHashMap() }
    /** 取/惰性创建 ZSet 索引空间 */
    private fun zsetIdx(cacheName: String) = zsetIndex.getOrPut(cacheName) { ConcurrentHashMap() }

    /** Set 索引的复合 key：避免不同属性间命名冲突 */
    private fun setKey(property: String, value: String) = "set:$property:$value"
    /** ZSet 索引的复合 key：同上，单层 zset 按属性独立 */
    private fun zsetKey(property: String) = "zset:$property"

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
        mainData.remove(cacheName)
        setIndex.remove(cacheName)
        zsetIndex.remove(cacheName)
    }

    override fun evictLocal(cacheName: String, id: Any) {
        val idStr = normalizePkField(id)
        val map = mainData[cacheName] ?: return
        map.remove(idStr) ?: return
        setIdx(cacheName).entries.forEach { (_, ids) -> ids.remove(idStr) }
        zsetIdx(cacheName).entries.forEach { (_, idToScore) -> idToScore.remove(idStr) }
    }

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? {
        @Suppress("UNCHECKED_CAST")
        return main(cacheName)[normalizePkField(id)] as? E
    }

    override fun existsById(cacheName: String, id: Any): Boolean =
        main(cacheName).containsKey(normalizePkField(id))

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        val id = entity.id ?: throw IllegalArgumentException("entity.id must not be null")
        val idStr = normalizePkField(id)
        main(cacheName)[idStr] = entity
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
        val entity = main(cacheName).remove(idStr) ?: return
        filterableProperties.forEach { prop ->
            getPropertyValue(entity, prop)?.let { value ->
                setIdx(cacheName)[setKey(prop, value.toString())]?.remove(idStr)
            }
        }
        sortableProperties.forEach { prop ->
            zsetIdx(cacheName)[zsetKey(prop)]?.remove(idStr)
        }
    }

    override fun <E : IIdEntity<*>> findByIds(cacheName: String, ids: Collection<*>, entityClass: KClass<E>): List<E> {
        if (ids.isEmpty()) return emptyList()
        val map = main(cacheName)
        return ids.mapNotNull {
            @Suppress("UNCHECKED_CAST")
            map[normalizePkField(it)] as? E
        }
    }

    override fun <PK, E : IIdEntity<PK>> listAll(cacheName: String, entityClass: KClass<E>): List<E> {
        @Suppress("UNCHECKED_CAST")
        return main(cacheName).values.toList().map { it as E }
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
     * - EQ / IN：toString 比对（兼容字符串化 id）
     * - GT/GE/LT/LE：toDouble 比对（数值字段）
     * - 其余：退化到 EQ
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
}
