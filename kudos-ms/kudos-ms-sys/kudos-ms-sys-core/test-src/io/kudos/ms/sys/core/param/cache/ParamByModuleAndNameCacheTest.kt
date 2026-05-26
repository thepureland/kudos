package io.kudos.ms.sys.core.param.cache

import io.kudos.ability.cache.common.kit.KeyValueCacheKit
import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.dao.SysParamDao
import io.kudos.ms.sys.core.param.model.po.SysParam
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * junit test for ParamByModuleAndNameCacheHandler
 *
 * Test data source: `ParamByModuleAndNameCacheTest.sql`
 *
 * @author K
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class ParamByModuleAndNameCacheTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var cacheHandler: ParamByModuleAndNameCache

    @Resource
    private lateinit var dao: SysParamDao

    private val newValue = "new-value"

    @Test
    fun reloadAll() {
        // Clear and reload the cache so it matches the DB
        cacheHandler.reloadAll(true)

        // Get current cached record
        var atomicServiceCode = "atomicServiceCode-a"
        var paramName = "paramName-1"
        val cacheItem = cacheHandler.getParam(atomicServiceCode, paramName)

        // Insert a new record into the DB
        val sysParamNew = insertNewRecordToDb()

        // Update an existing DB record
        val idUpdate = "9edc0327-99f1-4767-b42b-222222229755"
        dao.updateProperties(idUpdate, mapOf(SysParam::paramValue.name to newValue))

        // Delete a record from the DB
        val idDelete = "9edc0327-99f1-4767-b42b-333333339755"
        dao.deleteById(idDelete)

        // Reload the cache without clearing the old one
        cacheHandler.reloadAll(false)

        // Cached object should be a new instance (different memory address)
        assert(cacheItem !== cacheHandler.getParam(atomicServiceCode, paramName))

        // The newly inserted DB record should be in the cache
        assertNotNull(cacheHandler.getParam(sysParamNew.atomicServiceCode, sysParamNew.paramName))

        // The DB-updated record should also be updated in the cache
        atomicServiceCode = "atomicServiceCode-a"
        paramName = "paramName-2"
        val cacheItemUpdate = assertNotNull(cacheHandler.getParam(atomicServiceCode, paramName))
        assertEquals(newValue, cacheItemUpdate.paramValue)

        // The DB-deleted record should still remain in the cache (stale)
        atomicServiceCode = "atomicServiceCode-a"
        paramName = "paramName-3"
        assertNotNull(cacheHandler.getParam(atomicServiceCode, paramName))

        // Clear old cache and reload
        cacheHandler.reloadAll(true)

        // Now the DB-deleted record should no longer be in the cache
        atomicServiceCode = "atomicServiceCode-a"
        paramName = "paramName-3"
        assertNull(cacheHandler.getParam(atomicServiceCode, paramName))
    }

    @Test
    fun getParam() {
        var atomicServiceCode = "atomicServiceCode-a"
        var paramName = "paramName-1"
        assertNotNull(cacheHandler.getParam(atomicServiceCode, paramName))

        // Params with active=false should not be in the cache
        atomicServiceCode = "atomicServiceCode-d"
        paramName = "paramName-0"
        assertNull(cacheHandler.getParam(atomicServiceCode, paramName))
    }

    @Test
    fun syncOnInsert() {
        // Insert a new record into the DB
        val sysParam = insertNewRecordToDb()

        // Sync cache
        cacheHandler.syncOnInsert(sysParam, sysParam.id)

        // Verify the new record is in the cache
        val key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNotNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))
    }

    @Test
    fun syncOnUpdate() {
        // Update an existing DB record
        val id = "9edc0327-99f1-4767-b42b-444444449755"
        val success = dao.updateProperties(id, mapOf(SysParam::paramValue.name to newValue))
        assert(success)

        val sysParam = assertNotNull(dao.get(id))

        // Sync cache
        cacheHandler.syncOnUpdate(sysParam, id)

        // Verify the cached record
        val key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        var cacheItem = KeyValueCacheKit.getValue(cacheHandler.cacheName(), key) as SysParamCacheEntry?
        assertNotNull(cacheItem)
        assertEquals(newValue, cacheItem.paramValue)
        cacheItem = cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName)
        assertNotNull(cacheItem)
        assertEquals(newValue, cacheItem.paramValue)
    }

    @Test
    fun syncOnUpdateActive() {
        // Update from true to false
        var id = "9edc0327-99f1-4767-b42b-555555559755"
        var success = dao.updateProperties(id, mapOf(SysParam::active.name to false))
        assert(success)
        var sysParam = assertNotNull(dao.get(id))
        cacheHandler.syncOnUpdateActive(id, false)
        var key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))

        // Update from false to true
        id = "9edc0327-99f1-4767-b42b-000000009755"
        success = dao.updateProperties(id, mapOf(SysParam::active.name to true))
        assert(success)
        sysParam = assertNotNull(dao.get(id))
        cacheHandler.syncOnUpdateActive(id, true)
        key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNotNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
        assertNotNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))
    }

    @Test
    fun syncOnDelete() {
        val id = "9edc0327-99f1-4767-b42b-666666669755"
        val sysParam = assertNotNull(dao.get(id))

        // Delete the DB record
        val deleteSuccess = dao.deleteById(id)
        assert(deleteSuccess)

        // Sync cache
        cacheHandler.syncOnDelete(sysParam, id)

        // Verify it's gone from the cache
        val key = cacheHandler.getKey(sysParam.atomicServiceCode, sysParam.paramName)
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(sysParam.atomicServiceCode, sysParam.paramName))
    }

    @Test
    fun syncOnBatchDelete() {
        val id1 = "9edc0327-99f1-4767-b42b-777777779755"
        val id2 = "9edc0327-99f1-4767-b42b-888888889755"
        val ids = listOf(id1, id2)
        val atomicServiceCode1 = "atomicServiceCode-b"
        val atomicServiceCode2 = "atomicServiceCode-c"
        val paramName1 = "paramName-7"
        val paramName2 = "paramName-8"
        val moduleAndNames = listOf(
            Pair(atomicServiceCode1, paramName1),
            Pair(atomicServiceCode2, paramName2)
        )

        // Batch delete DB records
        val count = dao.batchDelete(ids)
        assertEquals(2, count)

        // Sync cache
        cacheHandler.syncOnBatchDelete(ids, moduleAndNames)

        // Verify they're gone from the cache
        var key = cacheHandler.getKey(atomicServiceCode1, paramName1)
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(atomicServiceCode1, paramName1))
        key = cacheHandler.getKey(atomicServiceCode2, paramName2)
        assertNull(KeyValueCacheKit.getValue(cacheHandler.cacheName(), key))
        assertNull(cacheHandler.getParam(atomicServiceCode2, paramName2))
    }

    private fun insertNewRecordToDb() : SysParam {
        val sysParam = SysParam().apply {
            paramName = "a_new_paramName"
            paramValue = "a_new_paramValue"
            atomicServiceCode = "atomicServiceCode-a"
        }
        dao.insert(sysParam)
        return sysParam
    }

}
