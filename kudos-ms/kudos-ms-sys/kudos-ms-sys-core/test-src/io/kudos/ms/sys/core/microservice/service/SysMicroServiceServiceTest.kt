package io.kudos.ms.sys.core.microservice.service

import io.kudos.ms.sys.common.microservice.vo.SysMicroServiceCacheEntry
import io.kudos.ms.sys.core.microservice.cache.SysMicroServiceHashCache
import io.kudos.ms.sys.core.microservice.model.po.SysMicroService
import io.kudos.ms.sys.core.microservice.service.iservice.ISysMicroServiceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysMicroServiceService
 *
 * Test data source: `SysMicroServiceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysMicroServiceService: ISysMicroServiceService

    @Resource
    private lateinit var sysMicroServiceHashCache: SysMicroServiceHashCache

    /** Microservice code in the seed data (physical primary key column is code, no id column; entity id is equivalent to code) */
    private val seededMicroServiceCode = "svc-microservice-test-1_2407"

    @Test
    fun get_byCodePrimaryKey_entityIdEqualsCode() {
        val row = sysMicroServiceService.get(seededMicroServiceCode)
        assertNotNull(row)
        assertEquals(seededMicroServiceCode, row.code)
        assertEquals(seededMicroServiceCode, row.id)
    }

    @Test
    fun get_withCacheEntryReturnType_delegatesToCache() {
        val fromGet = sysMicroServiceService.get(seededMicroServiceCode, SysMicroServiceCacheEntry::class)
        val fromCache = sysMicroServiceService.getMicroServiceFromCache(seededMicroServiceCode)
        assertNotNull(fromGet)
        assertNotNull(fromCache)
        assertEquals(fromCache.code, fromGet.code)
    }

    @Test
    fun deleteById_usesCodeColumn_notPhysicalIdColumn() {
        val unique = UUID.randomUUID().toString().replace("-", "").take(12)
        val code = "tc_del_$unique"
        val inserted = SysMicroService().apply {
            this.code = code
            name = "nm_$unique"
            context = "/test"
            atomicService = false
            parentCode = null
            remark = null
            active = true
            builtIn = false
        }
        assertEquals(code, sysMicroServiceService.insert(inserted))
        assertNotNull(sysMicroServiceService.get(code))
        assertTrue(sysMicroServiceService.deleteById(code))
        assertNull(sysMicroServiceService.get(code))
    }

    @Test
    fun getMicroServiceFromCache_and_updateActive() {
        val code = seededMicroServiceCode
        val cacheItem = sysMicroServiceService.getMicroServiceFromCache(code)
        assertNotNull(cacheItem)
        assertTrue(sysMicroServiceService.updateActive(code, false))
        assertTrue(sysMicroServiceService.updateActive(code, true))
    }

    @Test
    fun getAtomicServicesByParentCodeFromCache() {
        // The test SQL merges new rows into the DB, but the Hash full list does not auto-reload when the cache is non-empty; refresh before asserting
        sysMicroServiceHashCache.reloadAll(clear = true)
        val microServiceCode = seededMicroServiceCode
        val atomicServices = sysMicroServiceService.getAtomicServicesByParentCodeFromCache(microServiceCode)
        assertTrue(atomicServices.any { it.code == "svc-as-ms-test-1_2407" })
    }
}
