package io.kudos.ams.user.core.service

import io.kudos.ams.user.core.service.iservice.IUserAccountThirdService
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
        val userId = "11111111-0000-0000-0000-000000000001"
        val bindings = userAccountThirdService.getByUserAccountId(userId)
        assertTrue(bindings.size >= 2)
        assertTrue(bindings.all { it.userId == userId })
    }

    @Test
    fun getByProviderSubject() {
        val tenantId = "tenant-third-test-1"
        val accountProviderDictCode = "github"
        val accountProviderIssuer = "https://github.com"
        val subject = "github-user-001"

        val binding = userAccountThirdService.getByProviderSubject(
            tenantId = tenantId,
            accountProviderDictCode = accountProviderDictCode,
            accountProviderIssuer = accountProviderIssuer,
            subject = subject
        )
        assertNotNull(binding)
        assertEquals(subject, binding.subject)
        assertEquals("11111111-0000-0000-0000-000000000001", binding.userId)
    }
}