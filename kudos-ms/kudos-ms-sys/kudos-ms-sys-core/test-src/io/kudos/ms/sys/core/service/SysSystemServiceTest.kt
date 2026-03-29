package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysSystemService
 *
 * 测试数据来源：`SysSystemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysSystemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysSystemService: ISysSystemService

    @Test
    fun getSystemFromCache_and_getAllSystemsFromCache() {
        val code = "svc-system-test-1_6368"
        val cacheItem = sysSystemService.getSystemFromCache(code)
        assertNotNull(cacheItem)
        assertEquals(code, cacheItem.code)

        val allSystems = sysSystemService.getAllSystemsFromCache()
        assertTrue(allSystems.any { it.code == code })
    }

    @Test
    fun getSubSystemsFromCache() {
        val systemCode = "svc-system-test-1_6368"
        val subSystems = sysSystemService.getSubSystemsFromCache(systemCode)
        assertTrue(subSystems.any { it.code == "svc-subsystem-test-system-1_6368" })
    }

    @Test
    fun getSystemsExcludeSubSystemFromCache() {
        val rootCode = "svc-system-test-1_6368"
        val subCode = "svc-subsystem-test-system-1_6368"
        val list = sysSystemService.getSystemsExcludeSubSystemFromCache()
        assertTrue(list.any { it.code == rootCode })
        assertFalse(list.any { it.code == subCode })
        assertTrue(list.all { !it.subSystem })
    }

    @Test
    fun updateActive() {
        val code = "svc-system-test-1_6368"
        val success = sysSystemService.updateActive(code, false)
        assertTrue(success)

        // 更新后可再次更新回 true，验证写操作链路可用
        val success2 = sysSystemService.updateActive(code, true)
        assertTrue(success2)
    }
}
