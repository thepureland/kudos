package io.kudos.ms.auth.core.authentication.session

import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.core.authentication.session.model.AuthenticationSessionIssueCommand
import io.kudos.ms.auth.core.authentication.session.service.impl.AuthenticationSessionService
import io.kudos.ms.auth.core.authentication.session.spi.IAuthenticationContainerSessionPurger
import io.kudos.ms.auth.core.authentication.session.store.InMemoryAuthenticationSessionStore
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * The container-purge hand-off: every revocation reports itself once, and a purge that fails cannot take the
 * revocation down with it.
 */
internal class AuthenticationSessionPurgeTest {

    private val purger = RecordingPurger()
    private val store = InMemoryAuthenticationSessionStore()
    private val service = AuthenticationSessionService(store, containerSessionPurger = purger)

    @Test
    fun revokingOneSessionReportsExactlyThatSession() {
        val kept = issue()
        val revoked = issue()

        assertNotNull(service.revokeForUser(revoked.id, TENANT, USER, "device lost"))

        assertEquals(listOf(Triple(TENANT, USER, setOf(revoked.id))), purger.calls)
        assertTrue(assertNotNull(service.get(kept.id)).isActive())
    }

    @Test
    fun revokingEverythingReportsOneBatchRatherThanOneCallPerSession() {
        val first = issue()
        val second = issue()

        service.revokeAllForUser(TENANT, USER, "credentials replaced")

        // One call: the lookup an implementation makes is per user, so N calls would be N-1 lookups wasted.
        assertEquals(1, purger.calls.size)
        assertEquals(setOf(first.id, second.id), purger.calls.single().third)
    }

    @Test
    fun revokingAgainGivesAFailedPurgeAnotherChanceWhileRevokeAllStaysQuiet() {
        val session = issue()
        service.revokeForUser(session.id, TENANT, USER, "device lost")
        purger.calls.clear()

        service.revokeForUser(session.id, TENANT, USER, "device lost")

        // Deliberate: purging is best-effort and can fail silently, so a repeated revocation of the same
        // session re-reports it. Deleting a container record that is already gone is a no-op, and the retry
        // is worth more than the lookup it costs.
        assertEquals(listOf(Triple(TENANT, USER, setOf(session.id))), purger.calls)

        purger.calls.clear()
        service.revokeAllForUser(TENANT, USER, "credentials replaced")

        // Revoke-all works from the active sessions, of which there are none left, so it asks for nothing.
        assertEquals(emptyList(), purger.calls)
    }

    @Test
    fun aFailingPurgeLeavesTheRevocationStanding() {
        val exploding = AuthenticationSessionService(store) { _, _, _ ->
            error("session store unreachable")
        }
        val session = exploding.issue(issueCommand())

        val revoked = assertNotNull(exploding.revokeForUser(session.id, TENANT, USER, "device lost"))

        // The revocation is the security act and is already durable; every request re-validates the session,
        // so an unreachable container store must not turn it back into a failure.
        assertNotNull(revoked.revokedAt)
        assertTrue(!assertNotNull(exploding.get(session.id)).isActive())
    }

    @Test
    fun aDeploymentWithoutAPurgerBehavesAsBefore() {
        val plain = AuthenticationSessionService(store)
        val session = plain.issue(issueCommand())

        assertNotNull(plain.revokeForUser(session.id, TENANT, USER, "device lost"))
    }

    private fun issue() = service.issue(issueCommand())

    private fun issueCommand() = AuthenticationSessionIssueCommand(
        context = AuthenticationContext(
            userId = USER,
            tenantId = TENANT,
            authTime = Instant.now(),
            amr = setOf("password"),
            acr = "urn:kudos:acr:password",
        ),
        username = "alice",
    )

    private class RecordingPurger : IAuthenticationContainerSessionPurger {
        val calls = mutableListOf<Triple<String, String, Set<String>>>()

        override fun purge(tenantId: String, userId: String, logicalSessionIds: Set<String>) {
            calls += Triple(tenantId, userId, logicalSessionIds)
        }
    }

    private companion object {
        const val TENANT = "t-1"
        const val USER = "u-1"
    }
}
