package io.kudos.ms.auth.core.provider.invitation.service.impl

import io.kudos.ms.auth.common.provider.enums.ExternalJitPolicyEnum
import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.invitation.dao.AuthExternalIdentityInvitationDao
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreateCommand
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationCreated
import io.kudos.ms.auth.core.provider.invitation.model.AuthExternalIdentityInvitationReference
import io.kudos.ms.auth.core.provider.invitation.model.ExternalIdentityInvitationException
import io.kudos.ms.auth.core.provider.invitation.model.po.AuthExternalIdentityInvitation
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import io.kudos.ms.user.core.account.dao.UserAccountDao
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.LocalDateTime
import java.util.Base64
import java.util.Locale

@Service
@Transactional
open class ExternalIdentityInvitationService(
    private val invitationDao: AuthExternalIdentityInvitationDao,
    private val identityProviderDao: AuthIdentityProviderDao,
    private val userAccountDao: UserAccountDao,
) : IExternalIdentityInvitationService {

    private val secureRandom = SecureRandom()

    override fun create(command: AuthExternalIdentityInvitationCreateCommand): AuthExternalIdentityInvitationCreated {
        validateCreate(command)
        val provider = identityProviderDao.findActiveById(command.identityProviderId)
            ?: fail("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        if (provider.tenantId != command.tenantId) fail("EXTERNAL_PROVIDER_TENANT_MISMATCH")
        val jitPolicy = runCatching { ExternalJitPolicyEnum.valueOf(provider.jitPolicy.uppercase()) }.getOrNull()
        if (jitPolicy != ExternalJitPolicyEnum.INVITE_ONLY) fail("EXTERNAL_PROVIDER_NOT_INVITE_ONLY")
        val user = userAccountDao.get(command.userId)
            ?: fail("ACCOUNT_UNAVAILABLE")
        if (user.tenantId != command.tenantId) fail("ACCOUNT_TENANT_MISMATCH")
        if (user.active != true) fail("ACCOUNT_UNAVAILABLE")

        val now = LocalDateTime.now()
        if (!command.expiresAt.isAfter(now)) fail("EXTERNAL_INVITATION_EXPIRY_INVALID")
        val rawToken = newToken()
        val invitation = AuthExternalIdentityInvitation {
            tenantId = command.tenantId.trim()
            userId = command.userId.trim()
            identityProviderId = command.identityProviderId.trim()
            tokenHash = sha256(rawToken)
            expectedEmailHash = command.expectedEmail
                ?.takeIf { it.isNotBlank() }
                ?.let(::normalizeEmail)
                ?.let(::sha256)
            maxUses = ONE_TIME_USE
            usedCount = 0
            expiresAt = command.expiresAt
            active = true
            createUserId = command.actorUserId.trim()
            createReason = command.operationReason.trim().take(512)
            createTime = now
            updateTime = now
        }
        invitation.id = invitationDao.insert(invitation)
        return AuthExternalIdentityInvitationCreated(
            invitationId = invitation.id,
            token = rawToken,
            expiresAt = invitation.expiresAt,
            maxUses = invitation.maxUses,
        )
    }

    @Transactional(readOnly = true)
    override fun validateToken(
        token: String,
        tenantId: String,
        identityProviderId: String,
    ): AuthExternalIdentityInvitationReference {
        if (token.isBlank()) fail("EXTERNAL_INVITATION_REQUIRED")
        val invitation = invitationDao.findByTokenHash(sha256(token.trim()))
            ?: fail("EXTERNAL_INVITATION_NOT_AVAILABLE")
        requireScopeAndAvailability(invitation, tenantId, identityProviderId, LocalDateTime.now())
        return AuthExternalIdentityInvitationReference(invitation.id)
    }

    override fun consume(
        invitationId: String,
        tenantId: String,
        identityProviderId: String,
        principal: ExternalPrincipal,
    ): String {
        val now = LocalDateTime.now()
        if (principal.providerId != identityProviderId || principal.subject.isBlank()) {
            fail("EXTERNAL_INVITATION_PRINCIPAL_MISMATCH")
        }
        val invitation = invitationDao.get(invitationId)
            ?: fail("EXTERNAL_INVITATION_NOT_AVAILABLE")
        requireScopeAndAvailability(invitation, tenantId, identityProviderId, now)
        invitation.expectedEmailHash?.let { expectedHash ->
            val verifiedEmail = principal.email
                ?.takeIf { principal.emailVerified == true && it.isNotBlank() }
                ?: fail("EXTERNAL_INVITATION_VERIFIED_EMAIL_REQUIRED")
            if (!MessageDigest.isEqual(
                    expectedHash.toByteArray(Charsets.US_ASCII),
                    sha256(normalizeEmail(verifiedEmail)).toByteArray(Charsets.US_ASCII),
                )
            ) {
                fail("EXTERNAL_INVITATION_EMAIL_MISMATCH")
            }
        }
        if (!invitationDao.consume(
                invitation.id,
                tenantId,
                identityProviderId,
                now,
                sha256(principal.subject),
            )
        ) {
            fail("EXTERNAL_INVITATION_NOT_AVAILABLE")
        }
        return invitation.userId
    }

    override fun revoke(
        invitationId: String,
        tenantId: String,
        actorUserId: String,
        operationReason: String,
    ): Boolean {
        if (invitationId.isBlank() || tenantId.isBlank() || actorUserId.isBlank()) {
            fail("EXTERNAL_INVITATION_REQUEST_INVALID")
        }
        if (operationReason.isBlank() || operationReason.length > 512) {
            fail("EXTERNAL_INVITATION_REASON_INVALID")
        }
        val invitation = invitationDao.get(invitationId)
            ?: fail("EXTERNAL_INVITATION_NOT_FOUND")
        if (invitation.tenantId != tenantId) fail("EXTERNAL_INVITATION_NOT_FOUND")
        if (invitation.active != true) return false
        return invitationDao.revoke(
            invitation.id,
            tenantId,
            actorUserId.trim(),
            operationReason.trim(),
            LocalDateTime.now(),
        )
    }

    private fun validateCreate(command: AuthExternalIdentityInvitationCreateCommand) {
        if (command.tenantId.isBlank() || command.userId.isBlank() ||
            command.identityProviderId.isBlank() || command.actorUserId.isBlank()
        ) {
            fail("EXTERNAL_INVITATION_REQUEST_INVALID")
        }
        if (command.operationReason.isBlank() || command.operationReason.length > 512) {
            fail("EXTERNAL_INVITATION_REASON_INVALID")
        }
        command.expectedEmail?.let {
            if (it.isBlank() || it.length > 254 || '@' !in it) fail("EXTERNAL_INVITATION_EMAIL_INVALID")
        }
    }

    private fun requireScopeAndAvailability(
        invitation: AuthExternalIdentityInvitation,
        tenantId: String,
        identityProviderId: String,
        now: LocalDateTime,
    ) {
        if (invitation.tenantId != tenantId || invitation.identityProviderId != identityProviderId ||
            invitation.active != true || !invitation.expiresAt.isAfter(now) ||
            invitation.usedCount >= invitation.maxUses
        ) {
            fail("EXTERNAL_INVITATION_NOT_AVAILABLE")
        }
    }

    private fun newToken(): String = ByteArray(TOKEN_BYTES)
        .also(secureRandom::nextBytes)
        .let { Base64.getUrlEncoder().withoutPadding().encodeToString(it) }

    private fun normalizeEmail(email: String): String = email.trim().lowercase(Locale.ROOT)

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }

    private fun fail(errorCode: String): Nothing = throw ExternalIdentityInvitationException(errorCode)

    private companion object {
        const val TOKEN_BYTES = 32
        const val ONE_TIME_USE = 1
    }
}
