package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysPortalService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysPortalService
 *
 * 测试数据来源：`SysPortalServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysPortalServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysPortalService: ISysPortalService

    @Test
    fun getPortalByCode_and_getAllActivePortals() {
        val code = "svc-portal-test-1"
        val cacheItem = sysPortalService.getPortalByCode(code)
        assertNotNull(cacheItem)
        assertEquals(code, cacheItem.code)

        val activePortals = sysPortalService.getAllActivePortals()
        assertTrue(activePortals.any { it.code == code })
    }

    @Test
    fun getSubSystemsByPortalCode() {
        val portalCode = "svc-portal-test-1"
        val subSystems = sysPortalService.getSubSystemsByPortalCode(portalCode)
        assertTrue(subSystems.any { it.code == "svc-subsystem-test-portal-1" })
    }

    @Test
    fun updateActive() {
        val code = "svc-portal-test-1"
        val success = sysPortalService.updateActive(code, false)
        assertTrue(success)

        // 更新后可再次更新回 true，验证写操作链路可用
        val success2 = sysPortalService.updateActive(code, true)
        assertTrue(success2)
    }
}

