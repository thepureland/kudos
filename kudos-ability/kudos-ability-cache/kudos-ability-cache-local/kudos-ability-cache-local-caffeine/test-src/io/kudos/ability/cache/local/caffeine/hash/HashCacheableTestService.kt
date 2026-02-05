package io.kudos.ability.cache.local.caffeine.hash

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.base.lang.string.RandomStringKit
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.query.sort.Order
import org.springframework.stereotype.Service

/**
 * 供测试 [HashCacheableByPrimary] / [HashBatchCacheableByPrimary] 用的 Service：
 * 从内存 map 按 id 返回 [TestRow]，并模拟二级索引查询、按 sortScore 排序分页、条件+排序分页，
 * 便于与缓存的 listBySetIndex / listPageByZSetIndex / list 结果对比。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Service
open class HashCacheableTestService {

    fun putTestData(id: String, row: TestRow) {
        testData[id] = row
    }

    fun clearTestData() {
        testData.clear()
    }

    fun removeTestData(id: String) {
        testData.remove(id)
    }

    fun putTestDataWithTime(id: String, row: TestRowWithTime) {
        testDataWithTime[id] = row
    }

    fun removeTestDataWithTime(id: String) {
        testDataWithTime.remove(id)
    }

    fun clearTestDataWithTime() {
        testDataWithTime.clear()
    }

    /** 仿照 key-value 的 getFromDB：未启用缓存时每次调用返回新实例（name 不同），用于 NoHashCacheTest 断言无缓存。 */
    @HashCacheableByPrimary(
        cacheNames = ["testHash"],
        key = "#id",
        entityClass = TestRow::class,
        unless = "#result == null",
        filterableProperties = ["type"]
    )
    open fun getFromDB(id: String): TestRow {
        return TestRow(id = id, name = RandomStringKit.uuidWithoutDelimiter(), type = 1)
    }

    @HashCacheableByPrimary(
        cacheNames = ["testHash"],
        key = "#id",
        entityClass = TestRow::class,
        unless = "#result == null",
        filterableProperties = ["type"]
    )
    open fun getTestRowById(id: String): TestRow? {
        return testData[id]
    }

    @HashBatchCacheableByPrimary(
        cacheNames = ["testHash"],
        entityClass = TestRow::class,
        filterableProperties = ["type"]
    )
    open fun getTestRowsByIds(ids: List<String>): Map<String, TestRow?> {
        return ids.associateWith { testData[it] }
    }

    /** 按副属性（type，可选 status）等值查询：先查缓存，未命中则从内存取并回写；支持多一个属性 status 在内存中再筛。 */
    @HashCacheableBySecondary(
        cacheNames = ["testHash"],
        filterExpressions = ["#type"],
        entityClass = TestRow::class,
        filterableProperties = ["type", "status"]
    )
    open fun listTestRowsByType(type: Int, status: Int? = null): List<TestRow> {
        return testData.values.filter { it.type == type && (status == null || it.status == status) }
    }

    /** 模拟按 ZSet 索引（sortScore）排序分页：从内存排序后取一页。 */
    fun listTestRowsWithTimeBySortScorePage(offset: Long, limit: Long, desc: Boolean): List<TestRowWithTime> {
        val sorted = testDataWithTime.values
            .sortedByDescending { it.sortScore ?: 0.0 }
            .let { if (desc) it else it.reversed() }
        return sorted.drop(offset.toInt()).take(limit.toInt())
    }

    /** 模拟条件 + 排序 + 分页：从内存按 Criteria（仅支持单条件 type EQ）与 Order 过滤排序后取一页。 */
    fun listTestRowsWithTimePage(
        criteria: Criteria?,
        pageNo: Int,
        pageSize: Int,
        order: Order
    ): List<TestRowWithTime> {
        var list = testDataWithTime.values.toList()
        if (criteria != null && criteria.getCriterionGroups().isNotEmpty()) {
            for (group in criteria.getCriterionGroups()) {
                if (group is Criterion && group.operator == OperatorEnum.EQ && group.property == "type") {
                    val typeVal = group.value?.toString()?.toIntOrNull()
                    if (typeVal != null) list = list.filter { it.type == typeVal }
                    break
                }
            }
        }
        val prop = order.property
        val asc = order.direction == DirectionEnum.ASC
        list = when (prop) {
            "sortScore" -> if (asc) list.sortedBy { it.sortScore ?: 0.0 } else list.sortedByDescending { it.sortScore ?: 0.0 }
            "type" -> if (asc) list.sortedBy { it.type ?: 0 } else list.sortedByDescending { it.type ?: 0 }
            else -> list
        }
        val pNo = if (pageNo < 1) 1 else pageNo
        val pSize = if (pageSize < 1) 1 else pageSize
        val offset = (pNo - 1) * pSize
        return list.drop(offset).take(pSize)
    }

    companion object {
        private val testData = mutableMapOf<String, TestRow>()
        private val testDataWithTime = mutableMapOf<String, TestRowWithTime>()
    }
}
