package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysParamService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
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

    @Test
    fun getParamByAtomicServiceAndName() {
        val atomicServiceCode = "svc-module-param-test-1"
        val paramName = "svc-param-name-1"
        val cacheItem = sysParamService.getParamByAtomicServiceAndName(atomicServiceCode, paramName)
        assertNotNull(cacheItem)
    }

    @Test
    fun getParamsByAtomicServiceCode() {
        val atomicServiceCode = "svc-module-param-test-1"
        val params = sysParamService.getParamsByAtomicServiceCode(atomicServiceCode)
        assertTrue(params.any { it.paramName == "svc-param-name-1" })
    }

    @Test
    fun getParamValue() {
        val atomicServiceCode = "svc-module-param-test-1"
        val paramName = "svc-param-name-1"
        val value = sysParamService.getParamValue(atomicServiceCode, paramName)
        assertEquals("svc-param-value-1", value)
        
        val defaultValue = sysParamService.getParamValue(atomicServiceCode, "non-existent", "default")
        assertEquals("default", defaultValue)
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000038"
        assertTrue(sysParamService.updateActive(id, false))
        assertTrue(sysParamService.updateActive(id, true))
    }
}
