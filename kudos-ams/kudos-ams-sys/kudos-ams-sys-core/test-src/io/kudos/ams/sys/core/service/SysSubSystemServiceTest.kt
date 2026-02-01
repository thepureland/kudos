package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysSubSystemService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysSubSystemService
 *
 * 测试数据来源：`SysSubSystemServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysSubSystemServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysSubSystemService: ISysSubSystemService

    @Test
    fun getSubSystemByCode_and_updateActive() {
        val code = "svc-subsystem-test-1"
        val cacheItem = sysSubSystemService.getSubSystemByCode(code)
        assertNotNull(cacheItem)
        assertTrue(sysSubSystemService.updateActive(code, false))
        assertTrue(sysSubSystemService.updateActive(code, true))
    }

    @Test
    fun getSubSystemsByPortalCode() {
        val portalCode = "svc-portal-subsystem-test-1"
        val subs = sysSubSystemService.getSubSystemsByPortalCode(portalCode)
        assertTrue(subs.any { it.code == "svc-subsystem-test-1" })
    }

    @Test
    fun getMicroServicesBySubSystemCode() {
        val subSystemCode = "svc-subsystem-test-1"
        val microServices = sysSubSystemService.getMicroServicesBySubSystemCode(subSystemCode)
        assertTrue(microServices.any { it.code == "svc-ms-subsys-test-1" })
    }
}

