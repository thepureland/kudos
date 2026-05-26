package io.kudos.ms.sys.core.param.service

import io.kudos.ms.sys.common.param.vo.SysParamCacheEntry
import io.kudos.ms.sys.core.param.cache.ParamByModuleAndNameCache
import io.kudos.ms.sys.core.param.service.iservice.ISysParamService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * junit test for SysParamService
 *
 * Test data source: `SysParamServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysParamServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysParamService: ISysParamService

    @Resource
    private lateinit var paramByModuleAndNameCache: ParamByModuleAndNameCache

    private val seededId = "20000000-0000-0000-0000-000000008393"
    private val atomicServiceCode = "svc-module-param-test-1"
    private val paramName = "svc-param-name-1"

    /** Fetch entity via primary-key `get(id)`. */
    @Test
    fun get_byId_entity() {
        val row = sysParamService.get(seededId)
        assertNotNull(row)
        assertEquals(seededId, row.id)
    }

    /** `get(id, SysParamCacheEntry::class)` returns the cache VO by primary key (includes inactive rows; differs from the by-module-name cache). */
    @Test
    fun get_withCacheEntryReturnType_usesDao() {
        val entry = sysParamService.get(seededId, SysParamCacheEntry::class)
        assertNotNull(entry)
        assertEquals(seededId, entry.id)
        assertEquals(paramName, entry.paramName)
    }

    /** Read active parameters from SYS_PARAM_BY_MODULE_AND_NAME by module + parameter name. */
    @Test
    fun getParamFromCache_byModuleAndName() {
        paramByModuleAndNameCache.reloadAll(clear = true)
        val cacheItem = sysParamService.getParamFromCache(atomicServiceCode, paramName)
        assertNotNull(cacheItem)
        assertEquals(seededId, cacheItem.id)
    }

    /** For active rows, the by-id [SysParamCacheEntry] has the same primary key as the by-module cache entry. */
    @Test
    fun getParamFromCache_matchesGetByIdForActiveRow() {
        paramByModuleAndNameCache.reloadAll(clear = true)
        val byId = sysParamService.get(seededId, SysParamCacheEntry::class)
        val byKey = sysParamService.getParamFromCache(atomicServiceCode, paramName)
        assertNotNull(byId)
        assertNotNull(byKey)
        assertEquals(byId.id, byKey.id)
    }

    /** Query DB list rows by atomic service code. */
    @Test
    fun getParamsByAtomicServiceCode() {
        val params = sysParamService.getParamsByAtomicServiceCode(atomicServiceCode)
        assertTrue(params.any { it.paramName == paramName })
    }

    /** Parameter value chain: paramValue, defaultValue, then the call-site default. */
    @Test
    fun getParamValueFromCache() {
        paramByModuleAndNameCache.reloadAll(clear = true)
        val value = sysParamService.getParamValueFromCache(atomicServiceCode, paramName)
        assertEquals("svc-param-value-1", value)

        val defaultValue = sysParamService.getParamValueFromCache(atomicServiceCode, "non-existent", "default")
        assertEquals("default", defaultValue)
    }

    /** After disabling, the by-module cache should miss; after re-enabling, it should hit again. */
    @Test
    fun updateActive_syncsModuleNameCache() {
        paramByModuleAndNameCache.reloadAll(clear = true)
        assertNotNull(sysParamService.getParamFromCache(atomicServiceCode, paramName))

        assertTrue(sysParamService.updateActive(seededId, false))
        paramByModuleAndNameCache.reloadAll(clear = true)
        assertNull(sysParamService.getParamFromCache(atomicServiceCode, paramName))

        assertTrue(sysParamService.updateActive(seededId, true))
        paramByModuleAndNameCache.reloadAll(clear = true)
        assertNotNull(sysParamService.getParamFromCache(atomicServiceCode, paramName))
    }

    /** `updateActive` returns false when the primary key does not exist. */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysParamService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** `deleteById` returns false when the primary key does not exist. */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysParamService.deleteById("00000000-0000-0000-0000-000000000001"))
    }
}
