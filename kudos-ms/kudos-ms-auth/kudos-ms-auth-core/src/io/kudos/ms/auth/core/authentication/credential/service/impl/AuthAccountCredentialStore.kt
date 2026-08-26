package io.kudos.ms.auth.core.authentication.credential.service.impl

import io.kudos.ability.security.common.support.PasswordEncodingKit
import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSecretTypeEnum
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialStoreCommand
import io.kudos.ms.auth.core.authentication.credential.service.iservice.IAuthCredentialService
import io.kudos.ms.user.core.account.security.IAccountCredentialStore
import io.kudos.ms.user.core.account.security.IPasswordHistory
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service

/**
 * Makes `auth_credential` the login password's home, through the port the User domain exposes.
 *
 * The dependency runs auth → user and cannot run the other way, so the code that verifies a login cannot
 * simply call the credential's owner. Inverting it — user declares the port, auth implements it, Spring
 * composes the two when both are deployed — is the arrangement `IPasswordHistory` already uses here, and
 * reusing it keeps one pattern rather than inventing a second.
 *
 * Every operation needs a tenant and a user. Missing either is a wiring fault, not a failed login, so it
 * throws rather than quietly answering "no such password" — which would read to a caller as a wrong password.
 */
@Service
open class AuthAccountCredentialStore(
    private val credentialService: IAuthCredentialService,
    private val passwordEncoder: PasswordEncoder,
    private val passwordHistories: List<IPasswordHistory>,
) : IAccountCredentialStore {

    private val log = LogFactory.getLog(this::class)

    override fun hasPassword(context: PasswordPolicyContext): Boolean =
        credentialService.findActive(context.tenantIdOrFail(), context.userIdOrFail(), PASSWORD) != null

    override fun verifyPassword(plainPassword: String, context: PasswordPolicyContext): Boolean =
        credentialService.verifyWith(context.tenantIdOrFail(), context.userIdOrFail(), PASSWORD) { stored ->
            PasswordEncodingKit.matches(passwordEncoder, plainPassword, stored)
        }

    /**
     * Rotating and retiring the outgoing password into history happen together, in one pass over the stored
     * secret.
     *
     * History used to be recorded by the caller from `user_account.login_password`. Once the credential lives
     * here that column is empty, and the caller's history call would go quietly dead — reuse checks would keep
     * passing while silently consulting nothing. Doing it here keeps the check working and keeps the retiring
     * hash from being handed back out to arrange it.
     */
    override fun storePassword(encodedPassword: String, context: PasswordPolicyContext) {
        val tenantId = context.tenantIdOrFail()
        val userId = context.userIdOrFail()
        // History is filed before the compare-and-set resolves. That is still correct if this rotation loses:
        // losing means someone else replaced the same password, so it was retired either way.
        val rotated = credentialService.rotateWith(tenantId, userId, PASSWORD) { retiring ->
            recordHistory(retiring, context)
            encodedPassword
        }
        if (rotated) return
        // Either there was no password to rotate, or a concurrent change took the version. Enrolment states
        // that expectation as `expectedVersion = null`, so the second case surfaces as a version conflict
        // rather than overwriting the change that won.
        credentialService.store(
            AuthCredentialStoreCommand(
                tenantId = tenantId,
                userId = userId,
                type = PASSWORD,
                secretHashOrRef = encodedPassword,
                expectedVersion = null,
            )
        )
    }

    /**
     * Both halves happen inside the credential service: whether the stored encoding is out of date, and the
     * compare-and-set that replaces it. A password change that lands in between wins, and the upgrade is
     * dropped rather than reinstating the password that change replaced.
     */
    override fun upgradePasswordEncodingIfNeeded(
        plainPassword: String,
        context: PasswordPolicyContext,
    ): Boolean =
        credentialService.rotateWith(context.tenantIdOrFail(), context.userIdOrFail(), PASSWORD) { stored ->
            if (PasswordEncodingKit.upgradeEncoding(passwordEncoder, stored)) {
                passwordEncoder.encode(plainPassword)
            } else {
                null
            }
        }

    /** Best-effort: failing to file the outgoing password must not fail the password change itself. */
    private fun recordHistory(retiringPassword: String, context: PasswordPolicyContext) {
        if (!PasswordEncodingKit.looksLikeEncodedPassword(retiringPassword)) return
        passwordHistories.forEach { history ->
            runCatching { history.record(retiringPassword, context) }.onFailure { e ->
                log.warn("Failed to record the retiring password in history for ${context.userId}", e)
            }
        }
    }

    private fun PasswordPolicyContext.tenantIdOrFail(): String =
        tenantId?.takeIf { it.isNotBlank() } ?: error("A credential operation requires a tenant")

    private fun PasswordPolicyContext.userIdOrFail(): String =
        userId?.takeIf { it.isNotBlank() } ?: error("A credential operation requires a user")

    private companion object {
        val PASSWORD = AuthCredentialSecretTypeEnum.PASSWORD
    }
}
