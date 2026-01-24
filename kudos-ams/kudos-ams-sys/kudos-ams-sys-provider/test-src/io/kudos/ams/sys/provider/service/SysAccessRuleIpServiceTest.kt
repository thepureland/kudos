package io.kudos.ams.sys.provider.service

import io.kudos.ams.sys.provider.service.iservice.ISysAccessRuleIpService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * junit test for SysAccessRuleIpService
 *
 * 测试数据来源：`SysAccessRuleIpServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAccessRuleIpServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysAccessRuleIpService: ISysAccessRuleIpService

    @Test
    fun getIpsByRuleId() {
        val ruleId = "20000000-0000-0000-0000-000000000034"
        val ips = sysAccessRuleIpService.getIpsByRuleId(ruleId)
        assertTrue(ips.isNotEmpty())
    }

    @Test
    fun getIpsBySubSystemAndTenant() {
        val subSystemCode = "svc-subsys-arip-test-1"
        val tenantId = "20000000-0000-0000-0000-000000000034"
        val ips = sysAccessRuleIpService.getIpsBySubSystemAndTenant(subSystemCode, tenantId)
        assertTrue(ips.isNotEmpty())
    }

    @Test
    fun checkIpAccess() {
        val ip = 2130706433L // 127.0.0.1
        val subSystemCode = "svc-subsys-arip-test-1"
        val tenantId = "20000000-0000-0000-0000-000000000034"
        val allowed = sysAccessRuleIpService.checkIpAccess(ip, subSystemCode, tenantId)
        assertTrue(allowed)
    }
}
