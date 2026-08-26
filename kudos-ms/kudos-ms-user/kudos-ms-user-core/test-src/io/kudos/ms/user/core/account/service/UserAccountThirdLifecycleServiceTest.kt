package io.kudos.ms.user.core.account.service

import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.dao.UserAccountThirdDao
import io.kudos.ms.user.core.account.model.AdminExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountBindingException
import io.kudos.ms.user.core.account.model.UserAccountThirdAuditEvent
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.impl.UserAccountThirdService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdAuditService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/** Pure security-invariant tests for external identity binding and unbinding. */
internal class UserAccountThirdLifecycleServiceTest {

    private val bindingDao = mock(UserAccountThirdDao::class.java)
    private val accountDao = mock(UserAccountDao::class.java)
    private val auditService = mock(IUserAccountThirdAuditService::class.java)
    private val service = UserAccountThirdService(bindingDao, accountDao, auditService)

    private fun account(password: String = "hash") = UserAccount {
        id = "u-1"
        tenantId = "t-1"
        username = "alice"
        loginPassword = password
        supervisorId = "root"
        active = true
    }

    private fun command(subject: String = "subject-1") = ExternalAccountBindingCommand(
        userId = "u-1",
        tenantId = "t-1",
        identityProviderId = "provider-1",
        providerCode = "google",
        issuer = "https://accounts.google.com",
        subject = subject,
        displayName = "Alice External",
        email = "alice@example.com",
    )

    @Test
    fun bindCreatesOnlyServerVerifiedIdentityAndAuditsSuccess() {
        `when`(accountDao.get("u-1")).thenReturn(account())
        `when`(
            bindingDao.fetchByIdentityProviderSubject(
                "t-1", "provider-1", "https://accounts.google.com", "subject-1"
            )
        ).thenReturn(null)
        `when`(bindingDao.searchByUserId("u-1")).thenReturn(emptyList())
        `when`(bindingDao.insert(org.mockito.ArgumentMatchers.any(Any::class.java) ?: Any()))
            .thenReturn("binding-1")

        val binding = service.bindExternalIdentity(command())

        verify(accountDao).lockAccount("u-1")
        assertEquals("binding-1", binding.id)
        assertEquals("provider-1", binding.identityProviderId)
        assertEquals("subject-1", binding.subject)
        assertTrue(binding.active == true)
        val event = captureAudit()
        assertEquals("BIND", event.action)
        assertTrue(event.success)
    }

    @Test
    fun bindRejectsIdentityOwnedByAnotherUserAndAuditsDenial() {
        `when`(accountDao.get("u-1")).thenReturn(account())
        val otherBinding = UserAccountThird {
            id = "binding-other"
            userId = "u-2"
            tenantId = "t-1"
            identityProviderId = "provider-1"
            accountProviderDictCode = "google"
            accountProviderIssuer = "https://accounts.google.com"
            subject = "subject-1"
            active = true
        }
        `when`(
            bindingDao.fetchByIdentityProviderSubject(
                "t-1", "provider-1", "https://accounts.google.com", "subject-1"
            )
        ).thenReturn(otherBinding)

        val error = assertFailsWith<ExternalAccountBindingException> {
            service.bindExternalIdentity(command())
        }

        assertEquals("EXTERNAL_IDENTITY_ALREADY_BOUND", error.errorCode)
        val event = captureAudit()
        assertFalse(event.success)
        assertEquals("EXTERNAL_IDENTITY_ALREADY_BOUND", event.reason)
    }

    @Test
    fun unlinkRejectsLastMethodForPasswordlessAccount() {
        val account = account(password = "")
        val binding = binding()
        `when`(accountDao.get("u-1")).thenReturn(account)
        `when`(bindingDao.get("binding-1")).thenReturn(binding)
        `when`(bindingDao.searchActiveByUserId("u-1")).thenReturn(listOf(binding))

        val error = assertFailsWith<ExternalAccountBindingException> {
            service.unbindExternalIdentity("binding-1", "u-1", "t-1")
        }

        assertEquals("LAST_AUTHENTICATION_METHOD", error.errorCode)
        assertEquals("LAST_AUTHENTICATION_METHOD", captureAudit().reason)
    }

    @Test
    fun unlinkSoftDisablesBindingWhenAnotherMethodRemains() {
        val binding = binding()
        `when`(accountDao.get("u-1")).thenReturn(account())
        `when`(bindingDao.get("binding-1")).thenReturn(binding)
        `when`(bindingDao.searchActiveByUserId("u-1")).thenReturn(listOf(binding))
        `when`(
            bindingDao.updateOnly(
                binding,
                UserAccountThird::active.name,
                UserAccountThird::updateTime.name,
            )
        ).thenReturn(true)

        assertTrue(service.unbindExternalIdentity("binding-1", "u-1", "t-1"))
        assertFalse(binding.active == true)
        val event = captureAudit()
        assertEquals("UNBIND", event.action)
        assertTrue(event.success)
    }

    @Test
    fun adminPrebindUsesOperatorReasonAndSafeLifecycle() {
        `when`(accountDao.get("u-1")).thenReturn(account())
        `when`(
            bindingDao.fetchByIdentityProviderSubject(
                "t-1", "provider-1", "https://accounts.google.com", "subject-1"
            )
        ).thenReturn(null)
        `when`(bindingDao.searchByUserId("u-1")).thenReturn(emptyList())
        `when`(bindingDao.insert(org.mockito.ArgumentMatchers.any(Any::class.java) ?: Any()))
            .thenReturn("binding-1")

        service.prebindExternalIdentity(
            AdminExternalAccountBindingCommand(
                userId = "u-1",
                tenantId = "t-1",
                identityProviderId = "provider-1",
                providerCode = "google",
                issuer = "https://accounts.google.com",
                subject = "subject-1",
                actorUserId = "admin-1",
                operationReason = "Approved onboarding ticket K-42",
            )
        )

        val event = captureAudit()
        assertEquals("ADMIN_BIND", event.action)
        assertEquals("admin-1", event.actorUserId)
        assertEquals("Approved onboarding ticket K-42", event.operationReason)
        assertEquals(null, event.beforeSnapshot)
        assertEquals("subject-1", event.afterSnapshot?.subject)
    }

    @Test
    fun adminUnbindKeepsLastMethodGuardAndAuditsOperator() {
        val binding = binding()
        `when`(accountDao.get("u-1")).thenReturn(account())
        `when`(bindingDao.get("binding-1")).thenReturn(binding)
        `when`(bindingDao.searchActiveByUserId("u-1")).thenReturn(listOf(binding))
        `when`(
            bindingDao.updateOnly(
                binding,
                UserAccountThird::active.name,
                UserAccountThird::updateTime.name,
            )
        ).thenReturn(true)

        assertTrue(
            service.adminUnbindExternalIdentity(
                "binding-1",
                "t-1",
                "admin-1",
                "User offboarding request",
            )
        )

        val event = captureAudit()
        assertEquals("ADMIN_UNBIND", event.action)
        assertEquals("admin-1", event.actorUserId)
        assertEquals("User offboarding request", event.operationReason)
        assertTrue(event.beforeSnapshot?.active == true)
        assertFalse(event.afterSnapshot?.active == true)
    }

    @Test
    fun sameProviderCodeInDifferentProviderInstanceUsesIndependentSlot() {
        val existing = binding().apply { identityProviderId = "provider-other" }
        `when`(accountDao.get("u-1")).thenReturn(account())
        `when`(
            bindingDao.fetchByIdentityProviderSubject(
                "t-1", "provider-1", "https://accounts.google.com", "subject-1"
            )
        ).thenReturn(null)
        `when`(bindingDao.searchByUserId("u-1")).thenReturn(listOf(existing))
        `when`(bindingDao.insert(org.mockito.ArgumentMatchers.any(Any::class.java) ?: Any()))
            .thenReturn("binding-new")

        val created = service.bindExternalIdentity(command())

        assertEquals("binding-new", created.id)
        assertEquals("provider-1", created.identityProviderId)
    }

    private fun binding() = UserAccountThird {
        id = "binding-1"
        userId = "u-1"
        tenantId = "t-1"
        identityProviderId = "provider-1"
        accountProviderDictCode = "google"
        accountProviderIssuer = "https://accounts.google.com"
        subject = "subject-1"
        active = true
    }

    private fun captureAudit(): UserAccountThirdAuditEvent {
        val captor = ArgumentCaptor.forClass(UserAccountThirdAuditEvent::class.java)
        val fallback = UserAccountThirdAuditEvent(
            userId = "fallback",
            tenantId = "fallback",
            identityProviderId = null,
            providerCode = "fallback",
            subject = "fallback",
            action = "fallback",
            success = false,
            actorUserId = "fallback",
        )
        verify(auditService).record(captor.capture() ?: fallback)
        return captor.value
    }
}
