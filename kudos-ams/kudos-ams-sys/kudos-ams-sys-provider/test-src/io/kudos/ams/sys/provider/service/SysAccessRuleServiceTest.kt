package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysAccessRuleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.container.cache.CacheHandlerTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysAccessRuleService
 *
 * 测试数据来源：`V1.0.0.33__SysAccessRuleServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAccessRuleServiceTest : CacheHandlerTestBase() {

    @Resource
    private lateinit var sysAccessRuleService: ISysAccessRuleService

    @Test
    fun getAccessRuleByTenantAndSubSystem() {
        val tenantId = "20000000-0000-0000-0000-000000000033"
        val subSystemCode = "svc-subsys-ar-test-1"
        val portalCode = "svc-portal-ar-test-1"
        val rule = sysAccessRuleService.getAccessRuleByTenantAndSubSystem(tenantId, subSystemCode, portalCode)
        assertNotNull(rule)
    }

    @Test
    fun getAccessRulesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000033"
        val rules = sysAccessRuleService.getAccessRulesByTenantId(tenantId)
        assertTrue(rules.isNotEmpty())
    }

    @Test
    fun getAccessRulesBySubSystemCode() {
        val subSystemCode = "svc-subsys-ar-test-1"
        val rules = sysAccessRuleService.getAccessRulesBySubSystemCode(subSystemCode)
        assertTrue(rules.isNotEmpty())
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000033"
        assertTrue(sysAccessRuleService.updateActive(id, false))
        assertTrue(sysAccessRuleService.updateActive(id, true))
    }
}
