package io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice

import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnAssertion
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.VerifiedWebAuthnCredentialRegistration
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialAuditSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.WebAuthnCredentialSummary
import io.kudos.ms.auth.core.authentication.mfa.webauthn.model.po.AuthWebAuthnCredential

interface IWebAuthnCredentialService {
    fun registerVerified(command: VerifiedWebAuthnCredentialRegistration): WebAuthnCredentialSummary
    fun findActive(tenantId: String, credentialId: String): AuthWebAuthnCredential?
    fun findActiveUserIdByUserHandle(tenantId: String, userHandle: String): String?
    fun listActive(tenantId: String, userId: String): List<WebAuthnCredentialSummary>
    fun listForAudit(tenantId: String, userId: String): List<WebAuthnCredentialAuditSummary>
    fun isEnrolled(userId: String, tenantId: String): Boolean
    fun recordVerifiedAssertion(command: VerifiedWebAuthnAssertion): WebAuthnCredentialSummary
    fun rename(
        tenantId: String,
        userId: String,
        credentialId: String,
        displayName: String,
    ): WebAuthnCredentialSummary
    fun revoke(tenantId: String, userId: String, credentialId: String): Boolean
}
