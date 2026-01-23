package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysTenantResourceService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for SysTenantResourceService
 *
 * 测试数据来源：`V1.0.0.35__SysTenantResourceServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysTenantResourceServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysTenantResourceService: ISysTenantResourceService

    @Test
    fun getResourceIdsByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000035"
        val resourceIds = sysTenantResourceService.getResourceIdsByTenantId(tenantId)
        assertTrue(resourceIds.contains("20000000-0000-0000-0000-000000000035"))
    }

    @Test
    fun getTenantIdsByResourceId() {
        val resourceId = "20000000-0000-0000-0000-000000000035"
        val tenantIds = sysTenantResourceService.getTenantIdsByResourceId(resourceId)
        assertTrue(tenantIds.contains("20000000-0000-0000-0000-000000000035"))
    }

    @Test
    fun exists() {
        val tenantId = "20000000-0000-0000-0000-000000000035"
        val resourceId = "20000000-0000-0000-0000-000000000035"
        assertTrue(sysTenantResourceService.exists(tenantId, resourceId))
        assertFalse(sysTenantResourceService.exists(tenantId, "non-existent"))
    }

    @Test
    fun unbind() {
        val tenantId = "20000000-0000-0000-0000-000000000035"
        val resourceId = "20000000-0000-0000-0000-000000000035"
        val result = sysTenantResourceService.unbind(tenantId, resourceId)
        assertTrue(result)
        
        // 重新绑定
        val bindCount = sysTenantResourceService.batchBind(tenantId, listOf(resourceId))
        assertTrue(bindCount > 0)
    }
}
