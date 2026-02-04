package io.kudos.ability.cache.local.caffeine

import io.kudos.ability.cache.common.support.IIdEntitiesHashCache
import io.kudos.ability.cache.common.support.IIdEntitiesHashCacheSync
import io.kudos.base.lang.math.NumberKit
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.query.sort.Order
import io.kudos.base.support.IIdEntity
import kotlin.reflect.KClass
import java.lang.reflect.Method
import java.util.concurrent.ConcurrentHashMap

/**
 * Hash 缓存的 Caffeine 本地实现，内存结构模拟 Redis Hash + Set + ZSet；
 * 同时实现 [IIdEntitiesHashCacheSync] 供收到 Redis 通知后清理本地。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
class CaffeineIdEntitiesHashCache : IIdEntitiesHashCache, IIdEntitiesHashCacheSync {

    private val mainData = ConcurrentHashMap<String, ConcurrentHashMap<String, Any>>()
    private val setIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableSet<String>>>()
    private val zsetIndex = ConcurrentHashMap<String, ConcurrentHashMap<String, MutableMap<String, Double>>>()

    private fun main(cacheName: String) = mainData.getOrPut(cacheName) { ConcurrentHashMap() }
    private fun setIdx(cacheName: String) = setIndex.getOrPut(cacheName) { ConcurrentHashMap() }
    private fun zsetIdx(cacheName: String) = zsetIndex.getOrPut(cacheName) { ConcurrentHashMap() }

    private fun setKey(property: String, value: String) = "set:$property:$value"
    private fun zsetKey(property: String) = "zset:$property"

    private fun toDouble(value: Any): Double = when (value) {
        is Number -> value.toDouble()
        is String -> value.toDoubleOrNull() ?: Double.MIN_VALUE
        else -> Double.MIN_VALUE
    }

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
        val idStr = id.toString()
        val map = mainData[cacheName] ?: return
        val entity = map.remove(idStr) ?: return
        setIdx(cacheName).entries.forEach { (key, ids) -> ids.remove(idStr) }
        zsetIdx(cacheName).entries.forEach { (_, idToScore) -> idToScore.remove(idStr) }
    }

    override fun <PK, E : IIdEntity<PK>> getById(cacheName: String, id: PK, entityClass: KClass<E>): E? {
        @Suppress("UNCHECKED_CAST")
        return main(cacheName)[id.toString()] as? E
    }

    override fun <PK, E : IIdEntity<PK>> save(
        cacheName: String,
        entity: E,
        filterableProperties: Set<String>,
        sortableProperties: Set<String>
    ) {
        val id = entity.id ?: throw IllegalArgumentException("entity.id must not be null")
        val idStr = id.toString()
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
        val idStr = id.toString()
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
        return ids.mapNotNull { map[it.toString()] as? E }
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
            entities = if (desc) entities.sortedByDescending { getPropertyValue(it, prop)?.let { v -> toDouble(v) } ?: Double.MIN_VALUE }
            else entities.sortedBy { getPropertyValue(it, prop)?.let { v -> toDouble(v) } ?: Double.MIN_VALUE }
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
}
