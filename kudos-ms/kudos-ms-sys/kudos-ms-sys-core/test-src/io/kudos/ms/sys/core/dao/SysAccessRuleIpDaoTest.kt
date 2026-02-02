package io.kudos.ms.sys.core.dao

import io.kudos.ms.sys.common.vo.accessruleip.SysAccessRuleIpSearchPayload
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
        val searchPayload = SysAccessRuleIpSearchPayload().apply {
            this.tenantId = "40000000-0000-0000-0000-000000000110"
            this.systemCode = "svc-system-arip-dao-test-1"
            this.pageNo = 1
            this.pageSize = 10
        }
        val records = sysAccessRuleIpDao.pagingSearch(searchPayload)
        assertTrue(records.isNotEmpty())
        assertTrue(records.any { it.id == "40000000-0000-0000-0000-000000000113" })
    }

    @Test
    fun count() {
        val searchPayload = SysAccessRuleIpSearchPayload().apply {
            this.tenantId = "40000000-0000-0000-0000-000000000110"
            this.active = true
        }
        val count = sysAccessRuleIpDao.count(searchPayload)
        assertTrue(count >= 2)
    }

    @Test
    fun leftJoinSearch() {
        val searchPayload = SysAccessRuleIpSearchPayload().apply {
            this.id = "40000000-0000-0000-0000-000000000113"
        }
        val query = sysAccessRuleIpDao.leftJoinSearch(searchPayload)
        assertNotNull(query)
        val totalRecords = query.totalRecordsInAllPages
        assertTrue(totalRecords >= 1)
    }
}
