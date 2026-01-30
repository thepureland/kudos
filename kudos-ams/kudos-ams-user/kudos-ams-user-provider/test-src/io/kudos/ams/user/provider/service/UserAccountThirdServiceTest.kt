package io.kudos.ams.user.provider.service

import io.kudos.ams.user.provider.service.iservice.IUserAccountThirdService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * junit test for UserAccountThirdService
 *
 * 测试数据来源：`UserAccountThirdServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountThirdServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userAccountThirdService: IUserAccountThirdService

    @Test
    fun getByUserAccountId() {
        val userAccountId = "11111111-0000-0000-0000-000000000001"
        val bindings = userAccountThirdService.getByUserAccountId(userAccountId)
        assertTrue(bindings.size >= 2)
        assertTrue(bindings.all { it.userAccountId == userAccountId })
    }

    @Test
    fun getByProviderSubject() {
        val tenantId = "tenant-third-test-1"
        val subSysDictCode = "subsys-a"
        val accountProviderDictCode = "github"
        val providerIssuer = "https://github.com"
        val subject = "github-user-001"

        val binding = userAccountThirdService.getByProviderSubject(
            tenantId = tenantId,
            subSysDictCode = subSysDictCode,
            accountProviderDictCode = accountProviderDictCode,
            providerIssuer = providerIssuer,
            subject = subject
        )
        assertNotNull(binding)
        assertEquals(subject, binding.subject)
        assertEquals("11111111-0000-0000-0000-000000000001", binding.userAccountId)
    }
}
