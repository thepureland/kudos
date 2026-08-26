package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.dao.UserAccountThirdAuditDao
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.ms.user.core.account.service.iservice.IUserOrgUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.util.UUID
import java.util.concurrent.Callable
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@EnabledIfDockerInstalled
class ExternalAccountProvisioningServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var provisioningService: IExternalAccountProvisioningService

    @Resource
    private lateinit var userAccountService: IUserAccountService

    @Resource
    private lateinit var thirdService: IUserAccountThirdService

    @Resource
    private lateinit var auditDao: UserAccountThirdAuditDao

    @Resource
    private lateinit var userOrgUserService: IUserOrgUserService

    @Test
    fun provisionPersistsExternalOnlyAccountBindingAndJitAudit() {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(12)
        val command = command(suffix)

        val binding = provisioningService.provision(command)

        val user = userAccountService.get(binding.userId)
        assertNotNull(user)
        assertEquals(command.username, user.username)
        assertEquals("", user.loginPassword)
        assertEquals(command.defaultTimezone, user.defaultTimezone)
        assertEquals(command.defaultOrgId, user.orgId)
        assertEquals(setOf(command.defaultOrgId), userOrgUserService.getOrgIdsByUserId(user.id))
        assertTrue(userOrgUserService.getUserIdsByOrgId(command.defaultOrgId!!).contains(user.id))
        assertTrue(user.active)
        assertEquals(binding.id, provisioningService.provision(command).id)
        assertEquals(listOf("JIT_BIND"), auditDao.searchByBindingId(binding.id).map { it.action })
    }

    @Test
    fun concurrentProvisionLeavesExactlyOneAccountAndBinding() {
        val suffix = UUID.randomUUID().toString().replace("-", "").take(12)
        val command = command(suffix)
        val start = CountDownLatch(1)
        val pool = Executors.newFixedThreadPool(2)
        try {
            val futures = (1..2).map {
                pool.submit(Callable {
                    start.await(10, TimeUnit.SECONDS)
                    runCatching { provisioningService.provision(command) }
                })
            }
            start.countDown()
            val results = futures.map { it.get(20, TimeUnit.SECONDS) }

            assertTrue(results.any { it.isSuccess })
            val binding = thirdService.getByIdentityProviderSubject(
                command.tenantId,
                command.identityProviderId,
                command.issuer,
                command.subject,
            )
            assertNotNull(binding)
            assertEquals(1, userAccountService.getUsersByTenantId(command.tenantId).count { it.username == command.username })
            assertEquals(1, auditDao.searchByBindingId(binding.id).count { it.success && it.action == "JIT_BIND" })
        } finally {
            pool.shutdownNow()
        }
    }

    private fun command(suffix: String) = ExternalAccountProvisioningCommand(
        username = "jit_$suffix",
        tenantId = "jit-provisioning-tenant",
        identityProviderId = UUID.randomUUID().toString(),
        providerCode = "generic_oidc",
        issuer = "https://idp.example.com/$suffix",
        subject = "subject-$suffix",
        displayName = "JIT User",
        email = "jit-$suffix@example.com",
        defaultLocale = "en_US",
        defaultTimezone = "America/Argentina/Buenos_Aires",
        defaultOrgId = "10000000-0000-0000-0000-000000000001",
    )
}
