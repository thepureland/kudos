package io.kudos.ams.sys.core.service

import io.kudos.ams.sys.core.service.iservice.ISysDomainService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for SysDomainService
 *
 * 测试数据来源：`SysDomainServiceTest.sql`
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class SysDomainServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var sysDomainService: ISysDomainService

    @Test
    fun getDomainByName() {
        val domain = "svc-domain-test-1.com"
        val cacheItem = sysDomainService.getDomainByName(domain)
        assertNotNull(cacheItem)
    }

    @Test
    fun getDomainsByTenantId() {
        val tenantId = "20000000-0000-0000-0000-000000000032"
        val domains = sysDomainService.getDomainsByTenantId(tenantId)
        assertTrue(domains.any { it.domain == "svc-domain-test-1.com" })
    }

    @Test
    fun getDomainsBySubSystemCode() {
        val subSystemCode = "svc-subsys-domain-test-1"
        val domains = sysDomainService.getDomainsBySubSystemCode(subSystemCode)
        assertTrue(domains.any { it.subSystemCode == subSystemCode })
    }

    @Test
    fun getDomainsBySystemCode() {
        val systemCode = "svc-system-domain-test-1"
        val domains = sysDomainService.getDomainsBySystemCode(systemCode)
        assertTrue(domains.any { it.systemCode == systemCode })
    }

    @Test
    fun updateActive() {
        val id = "20000000-0000-0000-0000-000000000032"
        assertTrue(sysDomainService.updateActive(id, false))
        assertTrue(sysDomainService.updateActive(id, true))
    }
}
