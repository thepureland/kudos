package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysDataSourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDataSourceService
 *
 * 测试数据来源：`SysDataSourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDataSourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDataSourceService: ISysDataSourceService

    @Test
    fun getDataSource() {
        val tenantId = "20000000-0000-0000-0000-000000000026"
        val subSystemCode = "svc-subsys-ds-test-1"
        val microServiceCode = "svc-ms-ds-test-1"

        val cacheItem = sysDataSourceService.getDataSource(tenantId, subSystemCode, microServiceCode)
        assertNotNull(cacheItem)
    }

    @Test
    fun getDataSourcesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000026"
        val dataSources = sysDataSourceService.getDataSourcesByTenantId(tenantId)
        assertTrue(dataSources.any { it.id == "20000000-0000-0000-0000-000000000026" })
    }

    @Test
    fun getDataSourcesBySubSystemCode() {
        val subSystemCode = "svc-subsys-ds-test-1"
        val dataSources = sysDataSourceService.getDataSourcesBySubSystemCode(subSystemCode)
        assertTrue(dataSources.any { it.subSystemCode == subSystemCode })
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000026"
        assertTrue(sysDataSourceService.updateActive(id, false))
        assertTrue(sysDataSourceService.updateActive(id, true))
    }
}
