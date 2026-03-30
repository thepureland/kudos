package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.common.vo.param.SysParamCacheEntry
import io.kudos.ms.sys.core.cache.ParamByModuleAndNameCache
import io.kudos.ms.sys.core.service.iservice.ISysParamService
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
 * 测试数据来源：`SysParamServiceTest.sql`
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

    /** 按主键 `get(id)` 取实体。 */
    @Test
    fun get_byId_entity() {
        val row = sysParamService.get(seededId)
        assertNotNull(row)
        assertEquals(seededId, row.id)
    }

    /** `get(id, SysParamCacheEntry::class)` 按主键映射缓存载体（含未启用记录；与模块名缓存语义不同）。 */
    @Test
    fun get_withCacheEntryReturnType_usesDao() {
        val entry = sysParamService.get(seededId, SysParamCacheEntry::class)
        assertNotNull(entry)
        assertEquals(seededId, entry.id)
        assertEquals(paramName, entry.paramName)
    }

    /** 模块 + 参数名从 SYS_PARAM_BY_MODULE_AND_NAME 读取启用参数。 */
    @Test
    fun getParamFromCache_byModuleAndName() {
        paramByModuleAndNameCache.reloadAll(clear = true)
        val cacheItem = sysParamService.getParamFromCache(atomicServiceCode, paramName)
        assertNotNull(cacheItem)
        assertEquals(seededId, cacheItem.id)
    }

    /** 启用行下，按 id 的 [SysParamCacheEntry] 与按模块缓存项主键一致。 */
    @Test
    fun getParamFromCache_matchesGetByIdForActiveRow() {
        paramByModuleAndNameCache.reloadAll(clear = true)
        val byId = sysParamService.get(seededId, SysParamCacheEntry::class)
        val byKey = sysParamService.getParamFromCache(atomicServiceCode, paramName)
        assertNotNull(byId)
        assertNotNull(byKey)
        assertEquals(byId.id, byKey.id)
    }

    /** 按原子服务编码查库列表行。 */
    @Test
    fun getParamsByAtomicServiceCode() {
        val params = sysParamService.getParamsByAtomicServiceCode(atomicServiceCode)
        assertTrue(params.any { it.paramName == paramName })
    }

    /** 参数取值链：paramValue、defaultValue、入参默认值。 */
    @Test
    fun getParamValueFromCache() {
        paramByModuleAndNameCache.reloadAll(clear = true)
        val value = sysParamService.getParamValueFromCache(atomicServiceCode, paramName)
        assertEquals("svc-param-value-1", value)

        val defaultValue = sysParamService.getParamValueFromCache(atomicServiceCode, "non-existent", "default")
        assertEquals("default", defaultValue)
    }

    /** 停用后按模块缓存应取不到；恢复启用后可再命中。 */
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

    /** 主键不存在时 `updateActive` 返回 false。 */
    @Test
    fun updateActive_whenIdNotExists_returnsFalse() {
        assertFalse(sysParamService.updateActive("00000000-0000-0000-0000-000000000001", true))
    }

    /** 主键不存在时 `deleteById` 返回 false。 */
    @Test
    fun deleteById_returnsFalseWhenRowMissing() {
        assertFalse(sysParamService.deleteById("00000000-0000-0000-0000-000000000001"))
    }
}
