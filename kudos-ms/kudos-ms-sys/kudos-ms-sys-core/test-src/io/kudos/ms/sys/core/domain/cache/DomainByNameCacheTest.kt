package io.kudos.ms.sys.core.domain.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.core.domain.dao.SysDomainDao
import io.kudos.ms.sys.core.domain.model.po.SysDomain
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for DomainByNameCacheHandler
 *
 * Test data source: `DomainByNameCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class DomainByNameCacheTest : RdbAndRedisCacheTestBase() {
    
    @Resource
    private lateinit var cacheHandler: DomainByNameCache
    
    @Resource
    private lateinit var dao: SysDomainDao

    private val newName = "newName.com"

    @Test
    fun reloadAll() {
        // Clear and reload the cache so it matches the database
        cacheHandler.reloadAll(true)

        // Get the currently cached record
        val cacheItem = cacheHandler.getDomain("domain1.com")

        // Insert a new record into the database
        val sysDomainNew = insertNewRecordToDb()

        // Update an existing database record
        val idUpdate = "8309fe9a-8810-4a79-9cff-222222225724"
        dao.updateProperties(idUpdate, mapOf(SysDomain::domain.name to newName))

        // Delete a record from the database
        val idDelete = "8309fe9a-8810-4a79-9cff-333333335724"
        dao.deleteById(idDelete)

        // Reload the cache without clearing the existing entries
        cacheHandler.reloadAll(false)

        // The memory address of the previously cached record should change
        val cacheItem1 = cacheHandler.getDomain("domain1.com")
        assert(cacheItem !== cacheItem1)

        // Newly inserted database record should be present in the cache
        val cacheItemNew = cacheHandler.getDomain(sysDomainNew.domain)
        assertNotNull(cacheItemNew)

        // After rename, the new domain name should be present in the cache
        val cacheItemUpdate = cacheHandler.getDomain(newName)
        assertNotNull(cacheItemUpdate)

        // After rename, the old domain name should still be present in the cache
        var cacheItemOld = cacheHandler.getDomain("domain2.com")
        assertNotNull(cacheItemOld)

        // Records deleted from the database should still be in the cache
        var cacheItemDelete = cacheHandler.getDomain("domain3.com")
        assertNotNull(cacheItemDelete)


        // Clear old cache and reload
        cacheHandler.reloadAll(true)

        // After rename, the old domain name should no longer be in the cache
        cacheItemOld = cacheHandler.getDomain("domain2.com")
        assertNull(cacheItemOld)

        // Records deleted from the database should no longer be in the cache
        cacheItemDelete = cacheHandler.getDomain("domain3.com")
        assertNull(cacheItemDelete)
    }

    @Test
    fun getDomain() {
        var cacheItem = cacheHandler.getDomain("domain1.com")
        assertNotNull(cacheItem)

        // Records with active=false should not be in the cache
        cacheItem = cacheHandler.getDomain("domain0.com")
        assertNull(cacheItem)
    }

    @Test
    fun syncOnInsert() {
        // Insert a new record into the database
        val sysDomain = insertNewRecordToDb()

        // Sync cache
        cacheHandler.syncOnInsert(sysDomain, sysDomain.id)

        // Verify the new record exists in the cache
        val cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), sysDomain.domain)
        assertNotNull(cacheItem)
        val cacheItem2 = cacheHandler.getDomain(sysDomain.domain)
        assertNotNull(cacheItem2)
    }

    @Test
    fun syncOnUpdate() {
        // Update an existing record in the database
        val id = "8309fe9a-8810-4a79-9cff-444444445724"
        val success = dao.updateProperties(id, mapOf(SysDomain::domain.name to newName))
        assert(success)


        // Sync cache
        cacheHandler.syncOnUpdate(null, id)

        // Verify the cached record
        var cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), newName) as SysDomainCacheEntry?
        assertNotNull(cacheItem)
        assertEquals(newName, cacheItem.domain)
        cacheItem = cacheHandler.getDomain(newName)
        assertNotNull(cacheItem)
        assertEquals(newName, cacheItem.domain)
    }

    @Test
    fun syncOnDelete() {
        val id = "8309fe9a-8810-4a79-9cff-555555555724"
        val sysDomain = assertNotNull(dao.get(id))

        // Delete the record from the database
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // Sync cache
        cacheHandler.syncOnDelete(sysDomain, id)

        // Verify whether it still exists in the cache
        var cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), sysDomain.domain)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDomain(sysDomain.domain)
        assertNull(cacheItem)
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "8309fe9a-8810-4a79-9cff-666666665724"
        val id2 = "8309fe9a-8810-4a79-9cff-777777775724"
        val ids = listOf(id1, id2)
        val domain1 = "domain6.com"
        val domain2 = "domain7.com"
        val domains = setOf(domain1, domain2)

        // Batch delete records from the database
        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        // Sync cache
        cacheHandler.syncOnBatchDelete(ids, domains)

        // Verify whether they still exist in the cache
        var cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), domain1)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDomain(domain1)
        assertNull(cacheItem)
        cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), domain2)
        assertNull(cacheItem)
        cacheItem = cacheHandler.getDomain(domain2)
        assertNull(cacheItem)
    }
    
    private fun insertNewRecordToDb(): SysDomain {
        val sysDomain = SysDomain().apply { 
            domain = "a_new_domain.com"
            systemCode = "default"
        }
        dao.insert(sysDomain)
        return sysDomain
    }

}