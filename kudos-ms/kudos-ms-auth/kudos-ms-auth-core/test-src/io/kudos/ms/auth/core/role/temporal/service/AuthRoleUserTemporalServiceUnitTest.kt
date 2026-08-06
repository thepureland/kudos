package io.kudos.ms.auth.core.role.temporal.service

import io.kudos.ms.auth.core.policy.GrantCandidate
import io.kudos.ms.auth.core.policy.GrantRejection
import io.kudos.ms.auth.core.policy.iservice.IAuthGrantPolicyService
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.temporal.service.impl.AuthRoleUserTemporalService
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-logic unit test for [AuthRoleUserTemporalService] (collaborators mocked with Mockito; no
 * Spring container, no DB) — complements the container-backed [AuthRoleUserTemporalServiceTest] by
 * covering the branches the integration fixture cannot reach deterministically:
 *
 *  - getGrantsByRoleId: the active-flag computation across every window shape (open/open, past,
 *    future, current, start-only, end-only) and the (userId, startTime nulls-last) ordering;
 *  - bindTemporal: the window sanity guard, the policy-gate consultation (admission — tenant,
 *    existence, approval, SoD — is the gate's job; this class's job is to ask it and honour the
 *    verdict), the two temporal-only guards (permanent grants are not narrowed, delegation-chain
 *    rows are not replaced), and the in-place revive of a plain windowed/revoked row;
 *  - purgeExpired: the empty short-circuit (no event), and the group-by-role eviction over a
 *    multi-role expired set.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleUserTemporalServiceUnitTest {

    private val dao = mock(AuthRoleUserDao::class.java)
    private val grantPolicyService = mock(IAuthGrantPolicyService::class.java)
    private val eventPublisher = mock(org.springframework.context.ApplicationEventPublisher::class.java)

    private val service = AuthRoleUserTemporalService(dao).apply {
        inject("grantPolicyService", grantPolicyService)
        inject("eventPublisher", eventPublisher)
    }

    private fun AuthRoleUserTemporalService.inject(field: String, value: Any) {
        val f = AuthRoleUserTemporalService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    // Kotlin non-null params reject Mockito's null-returning matchers; these helpers register the
    // matcher (side effect) yet return a real non-null value so the Kotlin caller-side check passes.
    private fun anyRoleUser(): AuthRoleUser =
        ArgumentMatchers.any(AuthRoleUser::class.java) ?: AuthRoleUser { id = "x" }

    @Suppress("UNCHECKED_CAST")
    private fun anyIdCollection(): Collection<String> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<String>?) ?: emptyList()

    private fun anyEvent(): Any = ArgumentMatchers.any(Any::class.java) ?: Any()

    // searchExpiredGrants(now=now()) has a Kotlin default param, so the real call always passes a
    // freshly-computed instant; the stub must match it with a matcher (and coalesce the null).
    private fun anyDateTime(): LocalDateTime =
        ArgumentMatchers.any(LocalDateTime::class.java) ?: LocalDateTime.now()

    private fun grant(id: String, userId: String, start: LocalDateTime?, end: LocalDateTime?) =
        AuthRoleUser {
            this.id = id
            this.roleId = "role1"
            this.userId = userId
            this.startTime = start
            this.endTime = end
        }


    // ---------------------------------------------------------------- getGrantsByRoleId

    @Test
    fun getGrantsByRoleId_computesActiveFlagForEveryWindowShape() {
        val now = LocalDateTime.of(2026, 6, 15, 12, 0)
        val grants = listOf(
            grant("g-open", "u1", null, null),                              // permanent ⇒ active
            grant("g-current", "u1", now.minusDays(1), now.plusDays(1)),    // active
            grant("g-past", "u1", now.minusDays(2), now.minusDays(1)),      // expired ⇒ inactive
            grant("g-future", "u1", now.plusDays(1), now.plusDays(2)),      // not yet ⇒ inactive
            grant("g-startonly-active", "u2", now.minusDays(1), null),      // started, open end ⇒ active
            grant("g-startonly-future", "u2", now.plusDays(1), null),       // future start ⇒ inactive
            grant("g-endonly-active", "u2", null, now.plusDays(1)),         // open start, not ended ⇒ active
            grant("g-endonly-expired", "u2", null, now.minusDays(1)),       // open start, ended ⇒ inactive
        )
        whenCalled(dao.searchGrantsByRoleId("role1")).thenReturn(grants)

        val rows = service.getGrantsByRoleId("role1", now)
        val byId = rows.associateBy { it.id }
        assertTrue(byId.getValue("g-open").active)
        assertTrue(byId.getValue("g-current").active)
        assertFalse(byId.getValue("g-past").active)
        assertFalse(byId.getValue("g-future").active)
        assertTrue(byId.getValue("g-startonly-active").active)
        assertFalse(byId.getValue("g-startonly-future").active)
        assertTrue(byId.getValue("g-endonly-active").active)
        assertFalse(byId.getValue("g-endonly-expired").active)
    }

    @Test
    fun getGrantsByRoleId_sortsByUserThenStartTimeNullsLast() {
        val now = LocalDateTime.of(2026, 6, 15, 12, 0)
        // Deliberately out of order; expect (userId asc, startTime asc with null treated as MAX).
        val grants = listOf(
            grant("b-null", "userB", null, null),
            grant("a-early", "userA", now.minusDays(5), null),
            grant("a-null", "userA", null, null),
            grant("a-late", "userA", now.minusDays(1), null),
        )
        whenCalled(dao.searchGrantsByRoleId("role1")).thenReturn(grants)
        val rows = service.getGrantsByRoleId("role1", now)
        assertEquals(listOf("a-early", "a-late", "a-null", "b-null"), rows.map { it.id })
    }

    @Test
    fun getGrantsByRoleId_emptyReturnsEmpty() {
        whenCalled(dao.searchGrantsByRoleId("role1")).thenReturn(emptyList())
        assertTrue(service.getGrantsByRoleId("role1", LocalDateTime.now()).isEmpty())
    }

    @Test
    fun getGrantsByRoleId_defaultNow_usesCurrentTime() {
        // Exercise the `now = LocalDateTime.now()` default-arg overload (the $default bridge).
        val nearPast = LocalDateTime.now().minusMinutes(1)
        whenCalled(dao.searchGrantsByRoleId("role1"))
            .thenReturn(listOf(grant("g", "u1", nearPast, null)))
        val rows = service.getGrantsByRoleId("role1")
        assertEquals(1, rows.size)
        assertTrue(rows.single().active, "a window that opened a minute ago is active under default now()")
    }

    // ---------------------------------------------------------------- bindTemporal guards

    @Suppress("UNCHECKED_CAST")
    private fun anyCandidates(): Collection<GrantCandidate> =
        (ArgumentMatchers.any(Collection::class.java) as Collection<GrantCandidate>?) ?: emptyList()

    @Suppress("UNCHECKED_CAST")
    private fun anyRejections(): List<GrantRejection> =
        (ArgumentMatchers.any(List::class.java) as List<GrantRejection>?) ?: emptyList()

    @Test
    fun bindTemporal_startAfterEnd_rejected() {
        val now = LocalDateTime.now()
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("role1", "u1", now.plusDays(2), now.plusDays(1))
        }
        assertTrue(err.message!!.contains("start_time must not be after end_time"))
        verify(grantPolicyService, never()).screenGrants(anyCandidates())
    }

    /**
     * Admission — role/principal existence, tenant boundary, approval requirement, SoD — is the
     * policy gate's job, and a windowed grant faces all of it: a temporal grant with a generous
     * window was, among other things, an approval-workflow bypass while this path carried its own
     * inline subset of the checks. What this class owns is *consulting* the gate and honouring the
     * verdict, so that is what is pinned.
     */
    @Test
    fun bindTemporal_rejectedByPolicyGate_abortsBeforeAnyWrite() {
        whenCalled(grantPolicyService.assertNoRejection(anyRejections()))
            .thenThrow(IllegalArgumentException("cross-tenant grant"))
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("role1", "u1", null, null)
        }
        assertTrue(err.message!!.contains("cross-tenant"))
        verify(dao, never()).insert(anyRoleUser())
        verify(dao, never()).update(anyRoleUser())
        verify(eventPublisher, never()).publishEvent(anyEvent())
    }

    @Test
    fun bindTemporal_existingPermanentGrant_rejected() {
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1"))
            .thenReturn(listOf(grant("perm", "u1", null, null)))
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("role1", "u1", LocalDateTime.now(), null)
        }
        assertTrue(err.message!!.contains("already holds role"))
        verify(dao, never()).insert(anyRoleUser())
        verify(dao, never()).update(anyRoleUser())
    }

    /**
     * A delegation-chain row must never be silently replaced: rewriting it erases
     * granted_by / parent_grant_id, detaching the grant from the revocation cascade — a delegated
     * grant laundered into a direct one.
     */
    @Test
    fun bindTemporal_delegatedGrant_isNeverReplaced() {
        val delegated = grant("dlg", "u1", null, LocalDateTime.now().plusDays(5)).apply {
            grantedBy = "the-operator"
            parentGrantId = "parent-grant"
        }
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1")).thenReturn(listOf(delegated))
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("role1", "u1", null, null)
        }
        assertTrue(err.message!!.contains("delegated"))
        verify(dao, never()).insert(anyRoleUser())
        verify(dao, never()).update(anyRoleUser())
    }

    @Test
    fun bindTemporal_happyPath_insertsAndPublishesEvent() {
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1")).thenReturn(emptyList())
        whenCalled(dao.insert(anyRoleUser())).thenReturn("new-grant")

        val now = LocalDateTime.now()
        val id = service.bindTemporal("role1", "u1", now, now.plusDays(1))
        assertEquals("new-grant", id)
        verify(eventPublisher).publishEvent(AuthRoleUserRelationsChanged("role1", listOf("u1")))
    }

    /**
     * An existing plain windowed (or revoked) row is updated in place rather than replaced — the
     * row is the relationship's audit record, same convention as group membership. A revoked row
     * revives with the new window.
     */
    @Test
    fun bindTemporal_existingTemporalRow_updatedInPlaceAndRevived() {
        val old = grant("old", "u1", LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1)).apply {
            revoked = true
            revokeReason = "expired trial"
        }
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1")).thenReturn(listOf(old))
        whenCalled(dao.update(anyRoleUser())).thenReturn(true)

        val now = LocalDateTime.now()
        val id = service.bindTemporal("role1", "u1", now, now.plusDays(3))
        assertEquals("old", id, "the existing row's id survives — it was updated, not replaced")
        assertEquals(false, old.revoked)
        assertEquals(null, old.revokeReason)
        verify(dao, never()).insert(anyRoleUser())
        verify(dao, never()).deleteByRoleIdAndUserId(anyString(), anyString())
        verify(eventPublisher).publishEvent(AuthRoleUserRelationsChanged("role1", listOf("u1")))
    }

    // ---------------------------------------------------------------- purgeExpired

    @Test
    fun purgeExpired_empty_returnsZeroNoEvent() {
        whenCalled(dao.searchExpiredGrants(anyDateTime())).thenReturn(emptyList())
        assertEquals(0, service.purgeExpired())
        verify(dao, never()).batchDelete(anyIdCollection())
        verify(eventPublisher, never()).publishEvent(anyEvent())
    }

    @Test
    fun purgeExpired_groupsByRoleAndEvicts() {
        val expired = listOf(
            grantRole("e1", "rA", "u1"),
            grantRole("e2", "rA", "u2"),
            grantRole("e3", "rA", "u1"),  // duplicate user for rA → distinct in event
            grantRole("e4", "rB", "u3"),
        )
        whenCalled(dao.searchExpiredGrants(anyDateTime())).thenReturn(expired)
        whenCalled(dao.batchDelete(anyIdCollection())).thenReturn(4)

        val n = service.purgeExpired()
        assertEquals(4, n)
        verify(dao).batchDelete(listOf("e1", "e2", "e3", "e4"))
        verify(eventPublisher).publishEvent(AuthRoleUserRelationsChanged("rA", listOf("u1", "u2")))
        verify(eventPublisher).publishEvent(AuthRoleUserRelationsChanged("rB", listOf("u3")))
    }

    private fun grantRole(id: String, roleId: String, userId: String) = AuthRoleUser {
        this.id = id
        this.roleId = roleId
        this.userId = userId
    }
}
