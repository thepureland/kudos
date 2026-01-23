package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysModuleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysModuleService
 *
 * 测试数据来源：`V1.0.0.23__SysModuleServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysModuleServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysModuleService: ISysModuleService

    @Test
    fun getModuleByCode_and_updateActive() {
        val code = "svc-module-test-1"
        val cacheItem = sysModuleService.getModuleByCode(code)
        assertNotNull(cacheItem)
        assertTrue(sysModuleService.updateActive(code, false))
        assertTrue(sysModuleService.updateActive(code, true))
    }

    @Test
    fun getModulesByAtomicServiceCode() {
        val atomicServiceCode = "svc-as-module-test-1"
        val modules = sysModuleService.getModulesByAtomicServiceCode(atomicServiceCode)
        assertTrue(modules.any { it.code == "svc-module-test-1" })
    }
}
