package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysMicroServiceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysMicroServiceService
 *
 * 测试数据来源：`SysMicroServiceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysMicroServiceService: ISysMicroServiceService

    @Test
    fun getMicroServiceByCode_and_updateActive() {
        val code = "svc-microservice-test-1"
        val cacheItem = sysMicroServiceService.getMicroServiceByCode(code)
        assertNotNull(cacheItem)
        assertTrue(sysMicroServiceService.updateActive(code, false))
        assertTrue(sysMicroServiceService.updateActive(code, true))
    }

    @Test
    fun getAtomicServicesByMicroServiceCode() {
        val microServiceCode = "svc-microservice-test-1"
        val atomicServices = sysMicroServiceService.getAtomicServicesByMicroServiceCode(microServiceCode)
        assertTrue(atomicServices.any { it.code == "svc-as-ms-test-1" })
    }
}

