package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysSubSystemMicroServiceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysSubSystemMicroServiceService
 *
 * 测试数据来源：`V1.0.0.36__SysSubSystemMicroServiceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysSubSystemMicroServiceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysSubSystemMicroServiceService: ISysSubSystemMicroServiceService

    @Test
    fun getMicroServiceCodesBySubSystemCode() {
        val subSystemCode = "svc-subsys-ssms-test-1"
        val codes = sysSubSystemMicroServiceService.getMicroServiceCodesBySubSystemCode(subSystemCode)
        assertTrue(codes.contains("svc-ms-ssms-test-1"))
    }

    @Test
    fun getSubSystemCodesByMicroServiceCode() {
        val microServiceCode = "svc-ms-ssms-test-1"
        val codes = sysSubSystemMicroServiceService.getSubSystemCodesByMicroServiceCode(microServiceCode)
        assertTrue(codes.contains("svc-subsys-ssms-test-1"))
    }

    @Test
    fun exists() {
        val subSystemCode = "svc-subsys-ssms-test-1"
        val microServiceCode = "svc-ms-ssms-test-1"
        assertTrue(sysSubSystemMicroServiceService.exists(subSystemCode, microServiceCode))
        assertFalse(sysSubSystemMicroServiceService.exists(subSystemCode, "non-existent"))
    }

    @Test
    fun unbind() {
        val subSystemCode = "svc-subsys-ssms-test-1"
        val microServiceCode = "svc-ms-ssms-test-1"
        val result = sysSubSystemMicroServiceService.unbind(subSystemCode, microServiceCode)
        assertTrue(result)
        
        // 重新绑定
        val bindCount = sysSubSystemMicroServiceService.batchBind(subSystemCode, listOf(microServiceCode))
        assertTrue(bindCount > 0)
    }
}
