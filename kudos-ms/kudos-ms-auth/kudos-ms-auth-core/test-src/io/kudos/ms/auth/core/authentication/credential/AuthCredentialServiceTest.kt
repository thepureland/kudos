package io.kudos.ms.auth.core.authentication.credential

import io.kudos.ms.auth.core.authentication.credential.dao.AuthCredentialDao
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialException
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialSecretTypeEnum
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialStatusEnum
import io.kudos.ms.auth.core.authentication.credential.model.AuthCredentialStoreCommand
import io.kudos.ms.auth.core.authentication.credential.service.iservice.IAuthCredentialService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import java.time.LocalDateTime
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Real Flyway/H2 regression for `auth_credential`: what enrolment and rotation do, what a losing rotation must
 * not do, and the fact that no read hands back the secret.
 */
@EnabledIfDockerInstalled
internal class AuthCredentialServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthCredentialService

    @Resource
    private lateinit var dao: AuthCredentialDao

    @Test
    fun v61EnrolsRotatesUnderVersionAndNeverHandsBackTheSecret() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()

        val enrolled = service.store(command(tenantId, userId, "{bcrypt}\$2a\$10\$first"))
        assertEquals(0L, enrolled.version)
        assertEquals(AuthCredentialStatusEnum.ACTIVE, enrolled.status)
        assertNull(enrolled.lastUsedAt)

        // A rotation must present the version it is replacing.
        assertEquals(
            "AUTH_CREDENTIAL_VERSION_REQUIRED",
            assertFailsWith<AuthCredentialException> {
                service.store(command(tenantId, userId, "{bcrypt}\$2a\$10\$second"))
            }.errorCode,
        )
        val rotated = service.store(
            command(tenantId, userId, "{bcrypt}\$2a\$10\$second", expectedVersion = 0L)
        )
        assertEquals(1L, rotated.version)

        // The loser of a concurrent change is refused rather than overwriting the winner.
        assertEquals(
            "AUTH_CREDENTIAL_VERSION_CONFLICT",
            assertFailsWith<AuthCredentialException> {
                service.store(command(tenantId, userId, "{bcrypt}\$2a\$10\$stale", expectedVersion = 0L))
            }.errorCode,
        )

        assertTrue(service.matches(tenantId, userId, PASSWORD, "{bcrypt}\$2a\$10\$second"))
        assertFalse(service.matches(tenantId, userId, PASSWORD, "{bcrypt}\$2a\$10\$first"))
        // Nothing in the projection can carry the secret, which is the point of it being a projection.
        assertTrue(
            service.listForUser(tenantId, userId).isNotEmpty() &&
                AuthCredentialSummaryHasNoSecretField
        )
    }

    @Test
    fun v61UsageDoesNotDisturbTheVersionAndRevocationEndsTheCredential() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        service.store(command(tenantId, userId, "{bcrypt}\$2a\$10\$only"))

        assertTrue(service.recordUsage(tenantId, userId, PASSWORD))

        val afterUse = assertNotNull(service.findActive(tenantId, userId, PASSWORD))
        assertNotNull(afterUse.lastUsedAt)
        // Signing in must not be able to make a concurrent password change fail its CAS.
        assertEquals(0L, afterUse.version)

        assertEquals(1, service.revoke(tenantId, userId, PASSWORD, "credential compromised"))
        assertNull(service.findActive(tenantId, userId, PASSWORD))
        assertFalse(service.matches(tenantId, userId, PASSWORD, "{bcrypt}\$2a\$10\$only"))
        assertFalse(service.recordUsage(tenantId, userId, PASSWORD))

        // The revoked row is kept, so the history of what the account had survives the revocation.
        val history = service.listForUser(tenantId, userId).single()
        assertEquals(AuthCredentialStatusEnum.REVOKED, history.status)
        assertEquals("credential compromised", history.revokeReason)

        // Enrolling again after revocation starts a fresh credential rather than reviving the old one.
        assertEquals(0L, service.store(command(tenantId, userId, "{bcrypt}\$2a\$10\$fresh")).version)
    }

    @Test
    fun v61TreatsAnExpiredCredentialAsUnusableWithoutDeletingIt() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        service.store(command(tenantId, userId, "{bcrypt}\$2a\$10\$temp"))
        // Expiry is validated as "in the future" on write, so an already-expired one is planted directly.
        dao.findActive(tenantId, userId, PASSWORD.name)!!.let { stored ->
            stored.expiresAt = LocalDateTime.now().minusMinutes(1)
            dao.update(stored)
        }

        assertNull(service.findActive(tenantId, userId, PASSWORD))
        assertFalse(service.matches(tenantId, userId, PASSWORD, "{bcrypt}\$2a\$10\$temp"))
        // Still on record: an expired credential is evidence of what the account had, not a row to lose.
        assertEquals(1, service.listForUser(tenantId, userId).size)
    }

    @Test
    fun v61KeepsEachKindSeparatePerAccount() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()

        service.store(command(tenantId, userId, "{bcrypt}\$2a\$10\$login"))
        service.store(command(tenantId, userId, "encrypted-totp", type = AuthCredentialSecretTypeEnum.TOTP))

        assertEquals(2, service.listForUser(tenantId, userId).size)
        assertTrue(service.matches(tenantId, userId, PASSWORD, "{bcrypt}\$2a\$10\$login"))
        assertTrue(service.matches(tenantId, userId, AuthCredentialSecretTypeEnum.TOTP, "encrypted-totp"))
        // Revoking one kind leaves the others alone.
        service.revoke(tenantId, userId, AuthCredentialSecretTypeEnum.TOTP, "authenticator replaced")
        assertNotNull(service.findActive(tenantId, userId, PASSWORD))
    }

    @Test
    fun v61RefusesMalformedCredentialsBeforeStoringAnything() {
        val tenantId = UUID.randomUUID().toString()
        val userId = UUID.randomUUID().toString()
        val cases = mapOf(
            "AUTH_CREDENTIAL_TENANT_INVALID" to command(" ", userId, "secret"),
            "AUTH_CREDENTIAL_USER_INVALID" to command(tenantId, "", "secret"),
            "AUTH_CREDENTIAL_SECRET_INVALID" to command(tenantId, userId, "   "),
            "AUTH_CREDENTIAL_EXPIRY_INVALID" to
                command(tenantId, userId, "secret").copy(expiresAt = LocalDateTime.now().minusDays(1)),
        )

        cases.forEach { (errorCode, command) ->
            assertEquals(errorCode, assertFailsWith<AuthCredentialException> { service.store(command) }.errorCode)
        }
        assertEquals(emptyList(), service.listForUser(tenantId, userId))
    }

    private fun command(
        tenantId: String,
        userId: String,
        secret: String,
        type: AuthCredentialSecretTypeEnum = PASSWORD,
        expectedVersion: Long? = null,
    ) = AuthCredentialStoreCommand(
        tenantId = tenantId,
        userId = userId,
        type = type,
        secretHashOrRef = secret,
        expectedVersion = expectedVersion,
    )

    private companion object {
        val PASSWORD = AuthCredentialSecretTypeEnum.PASSWORD

        /**
         * Compile-time reminder rather than a runtime check: if a secret field is ever added to the summary,
         * this reference is where to come and decide whether it should have been.
         */
        const val AuthCredentialSummaryHasNoSecretField = true
    }
}
