package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.batch.hash.HashBatchCacheable
import io.kudos.ability.cache.common.aop.hash.HashCacheable
import org.springframework.stereotype.Service

/**
 * 供测试 [HashCacheable] 用的 Service：从内存 map 按 id 返回 [TestRow]，命中/未命中由切面走 Hash 缓存。
 * 使用 companion 共享 map，保证代理与目标实例访问同一数据源。
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

    @HashCacheable(
        cacheNames = ["testHash"],
        key = "#id",
        entityClass = TestRow::class,
        unless = "#result == null"
    )
    open fun getTestRowById(id: String): TestRow? {
        return testData[id]
    }

    @HashBatchCacheable(cacheNames = ["testHash"], entityClass = TestRow::class)
    open fun getTestRowsByIds(ids: List<String>): Map<String, TestRow?> {
        return ids.associateWith { testData[it] }
    }

    companion object {
        private val testData = mutableMapOf<String, TestRow>()
    }
}
