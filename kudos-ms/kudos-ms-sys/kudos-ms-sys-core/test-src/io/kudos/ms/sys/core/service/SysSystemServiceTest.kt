package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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
    fun getSystemByCode_and_getAllActiveSystems() {
        val code = "svc-system-test-1"
        val cacheItem = sysSystemService.getSystemByCode(code)
        assertNotNull(cacheItem)
        assertEquals(code, cacheItem.code)

        val activeSystems = sysSystemService.getAllActiveSystems()
        assertTrue(activeSystems.any { it.code == code })
    }

    @Test
    fun getSubSystemsBySystemCode() {
        val systemCode = "svc-system-test-1"
        val subSystems = sysSystemService.getSubSystemsBySystemCode(systemCode)
        assertTrue(subSystems.any { it.code == "svc-subsystem-test-system-1" })
    }

    @Test
    fun updateActive() {
        val code = "svc-system-test-1"
        val success = sysSystemService.updateActive(code, false)
        assertTrue(success)

        // 更新后可再次更新回 true，验证写操作链路可用
        val success2 = sysSystemService.updateActive(code, true)
        assertTrue(success2)
    }
}

