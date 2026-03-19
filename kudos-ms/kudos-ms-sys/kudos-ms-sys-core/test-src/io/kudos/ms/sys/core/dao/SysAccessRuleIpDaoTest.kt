package io.kudos.ms.sys.core.dao

import io.kudos.ms.sys.common.vo.accessruleip.request.SysAccessRuleIpQuery
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysAccessRuleIpDao
 *
 * 测试数据来源：`SysAccessRuleIpDaoTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysAccessRuleIpDaoTest : RdbTestBase() {

    @Resource
    private lateinit var sysAccessRuleIpDao: SysAccessRuleIpDao

    @Test
    fun pagingSearch() {
        val searchPayload = SysAccessRuleIpQuery(
            tenantId = "40000000-0000-0000-0000-000000002666",
            systemCode = "svc-system-arip-dao-test-1_3790",
        ).apply {
            pageNo = 1
            pageSize = 10

        }
        val records = sysAccessRuleIpDao.pagingSearch(searchPayload)
        assertTrue(records.isNotEmpty())
        assertTrue(records.any { it.id == "40000000-0000-0000-0000-000000002666" })
    }

    @Test
    fun count() {
        val searchPayload = SysAccessRuleIpQuery(
            tenantId = "40000000-0000-0000-0000-000000002666",
            active = true
        )
        val count = sysAccessRuleIpDao.count(searchPayload)
        assertTrue(count >= 2)
    }

    @Test
    fun leftJoinSearch() {
        val searchPayload = SysAccessRuleIpQuery(
            id = "40000000-0000-0000-0000-000000002666"
        )
        val query = sysAccessRuleIpDao.leftJoinSearch(searchPayload)
        assertNotNull(query)
        val totalRecords = query.totalRecordsInAllPages
        assertTrue(totalRecords >= 1)
    }
}
