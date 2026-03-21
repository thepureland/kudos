package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.aop.hash.HashCacheableByPrimary
import io.kudos.ability.cache.common.aop.hash.HashCacheableBySecondary
import io.kudos.ability.cache.common.batch.hash.HashBatchCacheableByPrimary
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.base.query.sort.DirectionEnum
import io.kudos.base.query.sort.Order
import org.springframework.stereotype.Service

/**
 * Test service used to verify hash cache behavior against an in-memory data source.
 */
@Service
open class HashCacheableTestService {

    /** Stores a test row in the in-memory source used by cache tests. */
    fun putTestData(id: String, row: TestRow) {
        testData[id] = row
    }

    /** Clears all in-memory test rows. */
    fun clearTestData() {
        testData.clear()
    }

    /** Removes one test row from the in-memory source. */
    fun removeTestData(id: String) {
        testData.remove(id)
    }

    /** Stores a sortable test row in the in-memory source used by page tests. */
    fun putTestDataWithTime(id: String, row: TestRowWithTime) {
        testDataWithTime[id] = row
    }

    /** Removes one sortable test row from the in-memory source. */
    fun removeTestDataWithTime(id: String) {
        testDataWithTime.remove(id)
    }

    /** Clears all sortable test rows from the in-memory source. */
    fun clearTestDataWithTime() {
        testDataWithTime.clear()
    }

    /** Loads a single row by id and writes unified secondary indexes for `type` and `status`. */
    @HashCacheableByPrimary(
        cacheNames = ["testHash"],
        key = "#id",
        entityClass = TestRow::class,
        unless = "#result == null",
        // 所有可以用來篩選的副屬性，因回寫緩存時要順帶維護其索引，所以同一緩存各方法中須保持一致的定義！！！
        filterableProperties = ["type", "status"]
    )
    open fun getTestRowById(id: String): TestRow? {
        return testData[id]?.copy()
    }

    /** Loads rows by ids in batch and writes unified secondary indexes for `type` and `status`. */
    @HashBatchCacheableByPrimary(
        cacheNames = ["testHash"],
        entityClass = TestRow::class,
        // 所有可以用來篩選的副屬性，因回寫緩存時要順帶維護其索引，所以同一緩存各方法中須保持一致的定義！！！
        filterableProperties = ["type", "status"]
    )
    open fun getTestRowsByIds(ids: List<String>): Map<String, TestRow?> {
        return ids.associateWith { testData[it]?.copy() }
    }

    /** Queries rows by `type` through a dedicated secondary index. */
    @HashCacheableBySecondary(
        cacheNames = ["testHash"],
        filterExpressions = ["#type"],
        entityClass = TestRow::class,
        // 所有可以用來篩選的副屬性，因回寫緩存時要順帶維護其索引，所以同一緩存各方法中須保持一致的定義！！！
        filterableProperties = ["type", "status"]
    )
    open fun listTestRowsByType(type: Int): List<TestRow> {
        return testData.values.filter { it.type == type }
    }

    /** Queries rows by the composite secondary indexes of `type` and `status`. */
    @HashCacheableBySecondary(
        cacheNames = ["testHash"],
        filterExpressions = ["#type", "#status"],
        entityClass = TestRow::class,
        // 所有可以用來篩選的副屬性，因回寫緩存時要順帶維護其索引，所以同一緩存各方法中須保持一致的定義！！！
        filterableProperties = ["type", "status"]
    )
    open fun listTestRowsByTypeAndStatus(type: Int, status: Int): List<TestRow> {
        return testData.values.filter { it.type == type && it.status == status }
    }

    /** Simulates paging by the sortable `sortScore` index using the in-memory source. */
    fun listTestRowsWithTimeBySortScorePage(offset: Long, limit: Long, desc: Boolean): List<TestRowWithTime> {
        val sorted = testDataWithTime.values
            .sortedByDescending { it.sortScore ?: 0.0 }
            .let { if (desc) it else it.reversed() }
        return sorted.drop(offset.toInt()).take(limit.toInt())
    }

    /**
     * Simulates criteria filtering, ordering and paging on the in-memory source.
     * Only a single `type = value` criterion is supported in this test helper.
     */
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
