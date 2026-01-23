package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysMicroServiceAtomicServiceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.CacheHandlerTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysMicroServiceAtomicServiceService
 *
 * 测试数据来源：`V1.0.0.37__SysMicroServiceAtomicServiceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysMicroServiceAtomicServiceServiceTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var sysMicroServiceAtomicServiceService: ISysMicroServiceAtomicServiceService

    @Test
    fun getAtomicServiceCodesByMicroServiceCode() {
        val microServiceCode = "svc-ms-msas-test-1"
        val codes = sysMicroServiceAtomicServiceService.getAtomicServiceCodesByMicroServiceCode(microServiceCode)
        assertTrue(codes.contains("svc-as-msas-test-1"))
    }

    @Test
    fun getMicroServiceCodesByAtomicServiceCode() {
        val atomicServiceCode = "svc-as-msas-test-1"
        val codes = sysMicroServiceAtomicServiceService.getMicroServiceCodesByAtomicServiceCode(atomicServiceCode)
        assertTrue(codes.contains("svc-ms-msas-test-1"))
    }

    @Test
    fun exists() {
        val microServiceCode = "svc-ms-msas-test-1"
        val atomicServiceCode = "svc-as-msas-test-1"
        assertTrue(sysMicroServiceAtomicServiceService.exists(microServiceCode, atomicServiceCode))
        assertFalse(sysMicroServiceAtomicServiceService.exists(microServiceCode, "non-existent"))
    }

    @Test
    fun unbind() {
        val microServiceCode = "svc-ms-msas-test-1"
        val atomicServiceCode = "svc-as-msas-test-1"
        val result = sysMicroServiceAtomicServiceService.unbind(microServiceCode, atomicServiceCode)
        assertTrue(result)
        
        // 重新绑定
        val bindCount = sysMicroServiceAtomicServiceService.batchBind(microServiceCode, listOf(atomicServiceCode))
        assertTrue(bindCount > 0)
    }
}
