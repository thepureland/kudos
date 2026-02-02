package io.kudos.ms.sys.core.service

import io.kudos.ms.sys.core.service.iservice.ISysAccessRuleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysAccessRuleService
 *
 * 测试数据来源：`SysAccessRuleServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAccessRuleServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysAccessRuleService: ISysAccessRuleService

    @Test
    fun getAccessRuleByTenantAndSystem() {
        val tenantId = "20000000-0000-0000-0000-000000009316"
        val systemCode = "svc-system-ar-test-0_8662"
        val rule = sysAccessRuleService.getAccessRuleByTenantAndSystem(tenantId, systemCode)
        assertNotNull(rule)
    }

    @Test
    fun getAccessRulesByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000009316"
        val rules = sysAccessRuleService.getAccessRulesByTenantId(tenantId)
        assertTrue(rules.isNotEmpty())
    }

    @Test
    fun getAccessRulesBySystemCode() {
        val systemCode = "svc-system-ar-test-0_8662"
        val rules = sysAccessRuleService.getAccessRulesBySystemCode(systemCode)
        assertTrue(rules.isNotEmpty())
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000009316"
        assertTrue(sysAccessRuleService.updateActive(id, false))
        assertTrue(sysAccessRuleService.updateActive(id, true))
    }
}
