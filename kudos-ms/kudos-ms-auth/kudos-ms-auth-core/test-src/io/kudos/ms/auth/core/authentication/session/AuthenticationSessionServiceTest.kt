package io.kudos.ms.auth.core.authentication.session

import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import io.kudos.ms.auth.core.authentication.session.store.IAuthenticationSessionStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

internal class AuthenticationSessionServiceTest {

    private val store = InMemoryAuthenticationSessionStore()
    private val service = AuthenticationSessionService(store)

    private fun command(tenantId: String = "t-1", userId: String = "u-1") = AuthenticationSessionIssueCommand(
        context = AuthenticationContext(
            userId = userId,
            tenantId = tenantId,
            authTime = Instant.parse("2026-08-24T10:00:00Z"),
            amr = setOf("password", "totp"),
            acr = "urn:kudos:acr:mfa",
            credentialVersion = 7,
            riskLevel = "LOW",
        ),
        username = "alice",
        clientId = "console",
        deviceId = "device-1",
        loginIp = 0x7F000001L,
        loginDevice = "PC",
        loginBrowser = "Chrome 126",
        loginOs = "Windows 11",
        userAgent = "test-agent",
    )

    @Test
    fun issue_copiesTrustedAuthenticationFactsAndCreatesIndependentId() {
        val issued = service.issue(command())

        assertTrue(issued.id.isNotBlank())
        assertEquals("u-1", issued.userId)
        assertEquals("t-1", issued.tenantId)
        assertEquals("alice", issued.username)
        assertEquals(setOf("password", "totp"), issued.amr)
        assertEquals("urn:kudos:acr:mfa", issued.acr)
        assertEquals(7, issued.credentialVersion)
        assertEquals("device-1", issued.deviceId)
        assertTrue(issued.isActive())
        assertEquals(issued, store.get(issued.id))
        assertTrue(issued.id != "any-servlet-session-id")
    }

    @Test
    fun touch_advancesVersionWithoutExtendingAbsoluteExpiry() {
        val issued = service.issue(command())

        val touched = assertNotNull(service.touch(issued.id))

        assertEquals(issued.version + 1, touched.version)
        assertEquals(issued.absoluteExpiresAt, touched.absoluteExpiresAt)
        assertTrue(!touched.lastSeenAt.isBefore(issued.lastSeenAt))
    }

    @Test
    fun elevateForUser_promotesSameSessionWithoutExtendingAbsoluteExpiry() {
        val issued = service.issue(command().copy(context = command().context.copy(
            amr = setOf("password"),
            acr = "urn:kudos:acr:password",
        )))
        val elevatedAt = Instant.now().plusSeconds(1)

        val elevated = assertNotNull(
            service.elevateForUser(
                issued.id,
                "t-1",
                "u-1",
                command().context.copy(
                    authTime = elevatedAt,
                    amr = setOf("password", "totp"),
                    acr = "urn:kudos:acr:mfa",
                    credentialVersion = 8,
                ),
            )
        )

        assertEquals(issued.id, elevated.id)
        assertEquals("urn:kudos:acr:mfa", elevated.acr)
        assertEquals(setOf("password", "totp"), elevated.amr)
        assertEquals(elevatedAt, elevated.authTime)
        assertEquals(8, elevated.credentialVersion)
        assertEquals(issued.absoluteExpiresAt, elevated.absoluteExpiresAt)
        assertEquals(issued.version + 1, elevated.version)
    }

    @Test
    fun elevateForUser_rejectsOwnerMismatchAndAssuranceDowngrade() {
        val issued = service.issue(command())

        assertNull(service.elevateForUser(issued.id, "t-1", "u-other", command().context.copy(userId = "u-other")))
        kotlin.test.assertFailsWith<IllegalArgumentException> {
            service.elevateForUser(
                issued.id,
                "t-1",
                "u-1",
                command().context.copy(amr = setOf("password"), acr = "urn:kudos:acr:password"),
            )
        }
        assertEquals("urn:kudos:acr:mfa", service.get(issued.id)?.acr)
    }

    @Test
    fun revoke_isOwnerBoundAndIdempotent() {
        val issued = service.issue(command())

        assertNull(service.revokeForUser(issued.id, "t-1", "u-other", "USER_LOGOUT"))
        assertNull(service.revokeForUser(issued.id, "t-other", "u-1", "USER_LOGOUT"))
        val revoked = assertNotNull(service.revokeForUser(issued.id, "t-1", "u-1", "USER_LOGOUT"))
        val repeated = assertNotNull(service.revokeForUser(issued.id, "t-1", "u-1", "DIFFERENT_REASON"))

        assertFalse(revoked.isActive())
        assertEquals("USER_LOGOUT", revoked.revokeReason)
        assertEquals(revoked, repeated)
    }

    @Test
    fun revoke_retriesATransientCompareAndSetConflict() {
        val delegate = InMemoryAuthenticationSessionStore()
        val store = object : IAuthenticationSessionStore by delegate {
            var rejectNextSave = true

            override fun save(session: AuthenticationSession, expectedVersion: Long): AuthenticationSession? {
                if (rejectNextSave) {
                    rejectNextSave = false
                    return null
                }
                return delegate.save(session, expectedVersion)
            }
        }
        val service = AuthenticationSessionService(store)
        val issued = service.issue(command())

        val revoked = assertNotNull(service.revokeForUser(issued.id, "t-1", "u-1", "USER_REVOKE"))

        assertFalse(revoked.isActive())
        assertEquals("USER_REVOKE", revoked.revokeReason)
    }

    @Test
    fun listForUser_returnsOnlyActiveSessionsForExactTenantOwner() {
        val first = service.issue(command())
        val second = service.issue(command())
        service.issue(command(userId = "u-other"))
        service.issue(command(tenantId = "t-other"))
        service.revokeForUser(first.id, "t-1", "u-1", "USER_REVOKE")

        val sessions = service.listForUser("t-1", "u-1")

        assertEquals(listOf(second.id), sessions.map(AuthenticationSession::id))
    }

    @Test
    fun revokeAllForUser_revokesOnlyExactTenantOwnerAndIsIdempotent() {
        val first = service.issue(command())
        val second = service.issue(command())
        val otherUser = service.issue(command(userId = "u-other"))
        val otherTenant = service.issue(command(tenantId = "t-other"))

        val revoked = service.revokeAllForUser("t-1", "u-1", "LOGIN_PASSWORD_CHANGED")
        val repeated = service.revokeAllForUser("t-1", "u-1", "DIFFERENT")

        assertEquals(setOf(first.id, second.id), revoked.map(AuthenticationSession::id).toSet())
        assertTrue(repeated.isEmpty())
        assertTrue(service.listForUser("t-1", "u-1").isEmpty())
        assertTrue(assertNotNull(service.get(otherUser.id)).isActive())
        assertTrue(assertNotNull(service.get(otherTenant.id)).isActive())
    }

    @Test
    fun get_marksIdleExpiredRecordRevoked() {
        val now = Instant.now()
        val expired = AuthenticationSession(
            id = "expired-1",
            tenantId = "t-1",
            userId = "u-1",
            authTime = now.minusSeconds(100),
            amr = setOf("password"),
            acr = "urn:kudos:acr:password",
            createdAt = now.minusSeconds(100),
            lastSeenAt = now.minusSeconds(100),
            idleExpiresAt = now.minusSeconds(1),
            absoluteExpiresAt = now.plusSeconds(100),
        )
        assertTrue(store.create(expired))

        val normalized = assertNotNull(service.get(expired.id))

        assertFalse(normalized.isActive())
        assertNotNull(normalized.revokedAt)
        assertEquals("IDLE_TIMEOUT", normalized.revokeReason)
    }

    @Test
    fun inMemoryStore_rejectsStaleVersion() {
        val issued = service.issue(command())
        val first = store.save(issued.copy(revokeReason = "first"), issued.version)
        assertNotNull(first)

        assertNull(store.save(issued.copy(revokeReason = "stale"), issued.version))
        assertEquals("first", store.get(issued.id)?.revokeReason)
    }
}
