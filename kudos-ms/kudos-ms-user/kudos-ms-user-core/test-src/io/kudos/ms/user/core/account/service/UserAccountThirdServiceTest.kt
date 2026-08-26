package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.dao.UserAccountThirdAuditDao
import io.kudos.ms.user.core.account.model.AdminExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import kotlin.test.assertFalse
import java.util.UUID

/**
 * junit test for UserAccountThirdService
 *
 * Test data source: `UserAccountThirdServiceTest.sql`.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class UserAccountThirdServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var userAccountThirdService: IUserAccountThirdService

    @Resource
    private lateinit var auditDao: UserAccountThirdAuditDao

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

    @Test
    fun getByIdentityProviderSubject() {
        val binding = userAccountThirdService.getByIdentityProviderSubject(
            tenantId = "tenant-third-test-1",
            identityProviderId = "33333333-0000-0000-0000-000000000001",
            accountProviderIssuer = "https://github.com",
            subject = "github-user-001",
        )

        assertNotNull(binding)
        assertEquals("11111111-0000-0000-0000-000000000001", binding.userId)
        assertEquals("33333333-0000-0000-0000-000000000001", binding.identityProviderId)
    }

    @Test
    fun bindAndSoftUnbindPersistAppendOnlyAudit() {
        val providerId = UUID.randomUUID().toString()
        val providerCode = "line-${UUID.randomUUID().toString().take(8)}"
        val subject = "line-${UUID.randomUUID()}"
        val binding = userAccountThirdService.bindExternalIdentity(
            ExternalAccountBindingCommand(
                userId = "11111111-0000-0000-0000-000000000001",
                tenantId = "tenant-third-test-1",
                identityProviderId = providerId,
                providerCode = providerCode,
                issuer = "https://access.line.me",
                subject = subject,
                displayName = "LINE User",
            )
        )

        assertTrue(binding.active == true)
        assertTrue(
            userAccountThirdService.unbindExternalIdentity(
                binding.id,
                "11111111-0000-0000-0000-000000000001",
                "tenant-third-test-1",
            )
        )
        assertFalse(userAccountThirdService.get(binding.id)?.active == true)
        val events = auditDao.searchByBindingId(binding.id).sortedBy { it.eventTime }
        assertEquals(listOf("BIND", "UNBIND"), events.map { it.action })
        assertTrue(events.all { it.subjectHash.length == 64 && it.subjectHash != subject })
    }

    @Test
    fun adminLifecyclePersistsOperatorReasonAndBeforeAfterSnapshots() {
        val providerId = UUID.randomUUID().toString()
        val subject = "admin-${UUID.randomUUID()}"
        val binding = userAccountThirdService.prebindExternalIdentity(
            AdminExternalAccountBindingCommand(
                userId = "11111111-0000-0000-0000-000000000001",
                tenantId = "tenant-third-test-1",
                identityProviderId = providerId,
                providerCode = "generic_oidc",
                issuer = "https://idp.example.com",
                subject = subject,
                actorUserId = "99999999-0000-0000-0000-000000000001",
                operationReason = "Approved onboarding ticket K-42",
            )
        )

        assertTrue(
            userAccountThirdService.adminUnbindExternalIdentity(
                binding.id,
                "tenant-third-test-1",
                "99999999-0000-0000-0000-000000000001",
                "Offboarding ticket K-43",
            )
        )

        val events = auditDao.searchByBindingId(binding.id).sortedBy { it.eventTime }
        assertEquals(listOf("ADMIN_BIND", "ADMIN_UNBIND"), events.map { it.action })
        assertEquals("Approved onboarding ticket K-42", events[0].operationReason)
        assertEquals("Offboarding ticket K-43", events[1].operationReason)
        assertEquals(null, events[0].beforeSnapshot)
        assertTrue(events[0].afterSnapshot!!.contains("subjectHash="))
        assertTrue(events[1].beforeSnapshot!!.contains("active=true"))
        assertTrue(events[1].afterSnapshot!!.contains("active=false"))
        assertTrue(events.all { event ->
            event.beforeSnapshot?.contains(subject) != true && event.afterSnapshot?.contains(subject) != true
        })
    }
}
