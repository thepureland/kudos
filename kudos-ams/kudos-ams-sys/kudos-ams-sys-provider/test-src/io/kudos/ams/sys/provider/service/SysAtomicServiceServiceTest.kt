package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysAtomicServiceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.CacheHandlerTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysAtomicServiceService
 *
 * 测试数据来源：`V1.0.0.22__SysAtomicServiceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAtomicServiceServiceTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var sysAtomicServiceService: ISysAtomicServiceService

    @Test
    fun getAtomicServiceByCode_and_updateActive() {
        val code = "svc-atomicservice-test-1"
        val cacheItem = sysAtomicServiceService.getAtomicServiceByCode(code)
        assertNotNull(cacheItem)
        assertTrue(sysAtomicServiceService.updateActive(code, false))
        assertTrue(sysAtomicServiceService.updateActive(code, true))
    }

    @Test
    fun getModulesByAtomicServiceCode() {
        val atomicServiceCode = "svc-atomicservice-test-1"
        val modules = sysAtomicServiceService.getModulesByAtomicServiceCode(atomicServiceCode)
        assertTrue(modules.any { it.code == "svc-module-as-test-1" })
    }
}

