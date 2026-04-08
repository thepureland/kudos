package io.kudos.ms.sys.core.accessrule.service
import io.kudos.base.error.ServiceException
import io.kudos.ms.sys.common.accessrule.enums.SysAccessRuleErrorCodeEnum
import io.kudos.ms.sys.common.accessrule.vo.request.SysAccessRuleFormCreate
import io.kudos.ms.sys.core.accessrule.service.iservice.ISysAccessRuleService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
        // 接口约定为 (systemCode, tenantId)，与方法名「ByTenantAndSystem」字面顺序不同，勿按名传参
        val rule = sysAccessRuleService.getAccessRuleByTenantAndSystem(systemCode, tenantId)
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

    @Test
    fun insertThrowsWhenDuplicateSystemCodeAndTenant() {
        val ex = assertFailsWith<ServiceException> {
            sysAccessRuleService.insert(
                SysAccessRuleFormCreate(
                    tenantId = "20000000-0000-0000-0000-000000009316",
                    systemCode = "svc-system-ar-test-0_8662",
                    accessRuleTypeDictCode = "0",
                    remark = "duplicate",
                ),
            )
        }
        assertEquals(SysAccessRuleErrorCodeEnum.ACCESS_RULE_ALREADY_EXISTS, ex.errorCode)
    }
}
