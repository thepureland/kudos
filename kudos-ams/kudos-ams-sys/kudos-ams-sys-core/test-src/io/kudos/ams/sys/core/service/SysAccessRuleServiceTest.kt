package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysAccessRuleService
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
    fun getAccessRuleByTenantAndSubSystem() {
        val tenantId = "20000000-0000-0000-0000-000000000033"
        val subSystemCode = "svc-subsys-ar-test-1"
        val systemCode = "svc-system-ar-test-1"
        val rule = sysAccessRuleService.getAccessRuleByTenantAndSubSystem(tenantId, subSystemCode, systemCode)
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
