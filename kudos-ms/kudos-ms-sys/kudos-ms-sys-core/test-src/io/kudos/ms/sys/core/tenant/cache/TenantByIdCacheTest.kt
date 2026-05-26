package io.kudos.ms.sys.core.tenant.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for TenantByIdCacheHandler
 *
 * Test data source: `TenantByIdCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class TenantByIdCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: TenantByIdCache

    @Resource
    private lateinit var sysTenantDao: SysTenantDao

    private val newTenantName = "New tenant name"

    @Test
    fun getTenantById() {
        // Existing
        var id = "118772a0-c053-4634-a5e5-111111115282"
        val cacheItem2 = cacheHandler.getTenantById(id)
        val cacheItem3 = cacheHandler.getTenantById(id)
        assertNotNull(cacheItem2)
        assert(cacheItem3 === cacheItem2)

        // Non-existing
        id = "no_exist_id"
        assertNull(cacheHandler.getTenantById(id))
    }

    @Test
    fun getTenantsByIds() {
        // All existing
        var id1 = "118772a0-c053-4634-a5e5-111111115282"
        var id2 = "118772a0-c053-4634-a5e5-222222225282"
        val result2 = cacheHandler.getTenantsByIds(listOf(id1, id2))
        val result3 = cacheHandler.getTenantsByIds(listOf(id1, id2))
        assert(result2.isNotEmpty())
        assert(result3 == result2)

        // Partially existing
        id1 = "no_exist_id-1"
        var cacheItems = cacheHandler.getTenantsByIds(listOf(id1, id2))
        assertEquals(1, cacheItems.size)

        // None existing
        id2 = "no_exist_id-2"
        cacheItems = cacheHandler.getTenantsByIds(listOf(id1, id2))
        assert(cacheItems.isEmpty())
    }

    @Test
    fun syncOnInsert() {
        // Insert a new record into the database
        val id = insertNewRecordToDb()

        // Sync cache
        cacheHandler.syncOnInsert(id)

        // Verify the new record is in the cache
        val cacheItem1 = KeyValueCacheKit.getValue(cacheHandler.cacheName(), id)
        assertNotNull(cacheItem1)
        val cacheItem2 = cacheHandler.getTenantById(id)
        assertNotNull(cacheItem2)
        val cacheItem3 = cacheHandler.getTenantById(id)
        assert(cacheItem2 === cacheItem3)
    }

    @Test
    fun syncOnUpdate() {
        // Update an existing record in the database
        val id = "118772a0-c053-4634-a5e5-222222225282"
        val success = sysTenantDao.updateProperties(id, mapOf(SysTenant::name.name to newTenantName))
        assert(success)

        // Sync cache
        cacheHandler.syncOnUpdate(id)

        // Verify the cached record
        val cacheItem1 = KeyValueCacheKit.getValue(cacheHandler.cacheName(), id) as SysTenantCacheEntry?
        assertNotNull(cacheItem1)
        assertEquals(newTenantName, cacheItem1.name)
        val cacheItem2 = cacheHandler.getTenantById(id)
        assertNotNull(cacheItem2)
        assertEquals(newTenantName, cacheItem2.name)
    }

    @Test
    fun syncOnDelete() {
        // Delete a record from the database
        val id = insertNewRecordToDb()
        val deleteSuccess = sysTenantDao.deleteById(id)
        assert(deleteSuccess)

        // Sync cache
        cacheHandler.syncOnDelete(id)

        // Verify whether it is in the cache
        val cacheItem1 = KeyValueCacheKit.getValue(cacheHandler.cacheName(), id)
        assertNull(cacheItem1)
        val cacheItem2 = cacheHandler.getTenantById(id)
        assertNull(cacheItem2)
    }

    @Test
    fun syncOnBatchDelete() {
        // Batch-delete records from the database
        val id1 = insertNewRecordToDb()
        val id2 = insertNewRecordToDb()
        val ids = listOf(id1, id2)
        val count = sysTenantDao.batchDelete(ids)
        assert(count == 2)

        // Sync cache
        cacheHandler.syncOnBatchDelete(ids)

        // Verify whether they are in the cache
        var cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), id1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getTenantById(id1)
        assertNull(cacheItem)
        cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), id2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getTenantById(id2)
        assertNull(cacheItem)
    }

    private fun insertNewRecordToDb(): String {
        val sysTenant = SysTenant().apply {
            name = "test_tenant_${System.currentTimeMillis()}"
        }
        return sysTenantDao.insert(sysTenant)
    }

}