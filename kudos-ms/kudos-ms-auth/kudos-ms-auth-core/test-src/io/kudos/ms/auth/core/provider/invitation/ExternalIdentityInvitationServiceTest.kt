package io.kudos.ms.auth.core.provider.invitation

import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.invitation.dao.AuthExternalIdentityInvitationDao
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreateCommand
import io.kudos.ms.auth.core.provider.invitation.model.ExternalIdentityInvitationException
import io.kudos.ms.auth.core.provider.invitation.model.po.AuthExternalIdentityInvitation
import io.kudos.ms.auth.core.provider.invitation.service.impl.ExternalIdentityInvitationService
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import org.mockito.ArgumentCaptor
import org.mockito.ArgumentMatchers.any
import org.mockito.ArgumentMatchers.anyString
import org.mockito.ArgumentMatchers.eq
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when`
import java.security.MessageDigest
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class ExternalIdentityInvitationServiceTest {
    private val invitationDao = mock(AuthExternalIdentityInvitationDao::class.java)
    private val providerDao = mock(AuthIdentityProviderDao::class.java)
    private val userDao = mock(UserAccountDao::class.java)
    private val service = ExternalIdentityInvitationService(invitationDao, providerDao, userDao)

    @Test
    fun createReturnsTokenOnceAndPersistsOnlyHashes() {
        arrangeProviderAndUser()
        `when`(invitationDao.insert(any(AuthExternalIdentityInvitation::class.java) ?: invitation()))
            .thenReturn("invitation-1")

        val created = service.create(command(expectedEmail = " Alice@Example.COM "))

        assertEquals("invitation-1", created.invitationId)
        assertEquals(1, created.maxUses)
        assertEquals(43, created.token.length)
        val captor = ArgumentCaptor.forClass(AuthExternalIdentityInvitation::class.java)
        verify(invitationDao).insert(captor.capture() ?: invitation())
        val persisted = captor.value
        assertEquals(sha256(created.token), persisted.tokenHash)
        assertEquals(sha256("alice@example.com"), persisted.expectedEmailHash)
        assertFalse(persisted.toString().contains(created.token))
        assertEquals("admin-1", persisted.createUserId)
        assertEquals("Approved onboarding", persisted.createReason)
    }

    @Test
    fun validateTokenPinsTenantAndProviderWithoutConsuming() {
        val stored = invitation(tokenHash = sha256("raw-token"))
        `when`(invitationDao.findByTokenHash(sha256("raw-token"))).thenReturn(stored)

        val reference = service.validateToken("raw-token", "t-1", "provider-1")

        assertEquals("invitation-1", reference.invitationId)
        verify(invitationDao, never()).consume(
            anyString(), anyString(), anyString(), any(LocalDateTime::class.java) ?: LocalDateTime.MIN, anyString()
        )
    }

    @Test
    fun consumeRequiresConfiguredVerifiedEmailAndClaimsExactlyOneUse() {
        val stored = invitation(expectedEmailHash = sha256("alice@example.com"))
        `when`(invitationDao.get("invitation-1")).thenReturn(stored)
        `when`(
            invitationDao.consume(
                eq("invitation-1") ?: "",
                eq("t-1") ?: "",
                eq("provider-1") ?: "",
                any(LocalDateTime::class.java) ?: LocalDateTime.MIN,
                eq(sha256("subject-1")) ?: "",
            )
        ).thenReturn(true)

        val userId = service.consume(
            "invitation-1",
            "t-1",
            "provider-1",
            principal(email = "ALICE@example.com", emailVerified = true),
        )

        assertEquals("u-1", userId)
    }

    @Test
    fun consumeRejectsUnverifiedOrMismatchingEmailBeforeWrite() {
        val stored = invitation(expectedEmailHash = sha256("alice@example.com"))
        `when`(invitationDao.get("invitation-1")).thenReturn(stored)

        val unverified = assertFailsWith<ExternalIdentityInvitationException> {
            service.consume(
                "invitation-1", "t-1", "provider-1",
                principal(email = "alice@example.com", emailVerified = false),
            )
        }
        assertEquals("EXTERNAL_INVITATION_VERIFIED_EMAIL_REQUIRED", unverified.errorCode)

        val mismatch = assertFailsWith<ExternalIdentityInvitationException> {
            service.consume(
                "invitation-1", "t-1", "provider-1",
                principal(email = "mallory@example.com", emailVerified = true),
            )
        }
        assertEquals("EXTERNAL_INVITATION_EMAIL_MISMATCH", mismatch.errorCode)
        verify(invitationDao, never()).consume(
            anyString(), anyString(), anyString(), any(LocalDateTime::class.java) ?: LocalDateTime.MIN, anyString()
        )
    }

    @Test
    fun losingAtomicConsumeIsReportedAsReplay() {
        val stored = invitation()
        `when`(invitationDao.get("invitation-1")).thenReturn(stored)
        `when`(
            invitationDao.consume(
                eq("invitation-1") ?: "",
                eq("t-1") ?: "",
                eq("provider-1") ?: "",
                any(LocalDateTime::class.java) ?: LocalDateTime.MIN,
                anyString(),
            )
        ).thenReturn(false)

        val error = assertFailsWith<ExternalIdentityInvitationException> {
            service.consume("invitation-1", "t-1", "provider-1", principal())
        }

        assertEquals("EXTERNAL_INVITATION_NOT_AVAILABLE", error.errorCode)
    }

    private fun arrangeProviderAndUser() {
        `when`(providerDao.findActiveById("provider-1")).thenReturn(AuthIdentityProvider {
            id = "provider-1"
            tenantId = "t-1"
            templateId = "google"
            code = "google-main"
            displayName = "Google"
            clientId = "client"
            jitPolicy = "INVITE_ONLY"
            linkPolicy = "BOUND_ONLY"
            active = true
        })
        `when`(userDao.get("u-1")).thenReturn(UserAccount {
            id = "u-1"
            tenantId = "t-1"
            username = "alice"
            loginPassword = "unused"
            supervisorId = ""
            active = true
        })
    }

    private fun command(expectedEmail: String? = null) = AuthExternalIdentityInvitationCreateCommand(
        tenantId = "t-1",
        userId = "u-1",
        identityProviderId = "provider-1",
        expectedEmail = expectedEmail,
        expiresAt = LocalDateTime.now().plusHours(1),
        actorUserId = "admin-1",
        operationReason = "Approved onboarding",
    )

    private fun invitation(
        tokenHash: String = sha256("raw-token"),
        expectedEmailHash: String? = null,
    ) = AuthExternalIdentityInvitation {
        id = "invitation-1"
        tenantId = "t-1"
        userId = "u-1"
        identityProviderId = "provider-1"
        this.tokenHash = tokenHash
        this.expectedEmailHash = expectedEmailHash
        maxUses = 1
        usedCount = 0
        expiresAt = LocalDateTime.now().plusHours(1)
        active = true
        createUserId = "admin-1"
        createReason = "Approved onboarding"
        createTime = LocalDateTime.now()
    }

    private fun principal(
        email: String? = null,
        emailVerified: Boolean? = null,
    ) = ExternalPrincipal(
        providerId = "provider-1",
        protocol = ExternalProtocolEnum.OIDC,
        issuer = "https://issuer",
        subject = "subject-1",
        email = email,
        emailVerified = emailVerified,
    )

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
