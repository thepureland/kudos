package io.kudos.ms.auth.core.role.temporal.service

import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.role.event.AuthRoleUserRelationsChanged
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import io.kudos.ms.auth.core.role.service.impl.AuthRoleUserService
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
 *  - bindTemporal: each guard (start-after-end, role-not-found, existing-permanent-grant, SoD), the
 *    happy path (delete-then-insert + relations-changed event), and the NULL-bound branches that
 *    skip the start/end comparison;
 *  - purgeExpired: the empty short-circuit (no event), and the group-by-role eviction over a
 *    multi-role expired set.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleUserTemporalServiceUnitTest {

    private val dao = mock(AuthRoleUserDao::class.java)
    private val authRoleDao = mock(AuthRoleDao::class.java)
    private val authRoleUserService = mock(AuthRoleUserService::class.java)
    private val eventPublisher = mock(org.springframework.context.ApplicationEventPublisher::class.java)

    private val service = AuthRoleUserTemporalService(dao).apply {
        inject("authRoleDao", authRoleDao)
        inject("authRoleUserService", authRoleUserService)
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

    private fun role(id: String, tenant: String = "t1") = AuthRole {
        this.id = id
        this.tenantId = tenant
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

    @Test
    fun bindTemporal_startAfterEnd_rejected() {
        val now = LocalDateTime.now()
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("role1", "u1", now.plusDays(2), now.plusDays(1))
        }
        assertTrue(err.message!!.contains("start_time must not be after end_time"))
        verify(authRoleDao, never()).get(anyString())
    }

    @Test
    fun bindTemporal_roleNotFound_rejected() {
        whenCalled(authRoleDao.get("ghost")).thenReturn(null)
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("ghost", "u1", null, null)
        }
        assertTrue(err.message!!.contains("Role not found: ghost"))
    }

    @Test
    fun bindTemporal_existingPermanentGrant_rejected() {
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1"))
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1"))
            .thenReturn(listOf(grant("perm", "u1", null, null)))
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("role1", "u1", LocalDateTime.now(), null)
        }
        assertTrue(err.message!!.contains("already holds role"))
        verify(dao, never()).insert(anyRoleUser())
    }

    @Test
    fun bindTemporal_sodViolation_rejected() {
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1", "tenantA"))
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1")).thenReturn(emptyList())
        whenCalled(authRoleUserService.findSodViolationMessage("tenantA", "role1", "u1"))
            .thenReturn("conflict with role X")
        val err = assertFailsWith<IllegalArgumentException> {
            service.bindTemporal("role1", "u1", null, null)
        }
        assertTrue(err.message!!.contains("SoD constraint violation"))
        verify(dao, never()).insert(anyRoleUser())
    }

    @Test
    fun bindTemporal_happyPath_replacesAndPublishesEvent() {
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1", "tenantA"))
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1")).thenReturn(emptyList())
        whenCalled(authRoleUserService.findSodViolationMessage("tenantA", "role1", "u1")).thenReturn(null)
        whenCalled(dao.deleteByRoleIdAndUserId("role1", "u1")).thenReturn(0)
        whenCalled(dao.insert(anyRoleUser())).thenReturn("new-grant")

        val now = LocalDateTime.now()
        val id = service.bindTemporal("role1", "u1", now, now.plusDays(1))
        assertEquals("new-grant", id)
        verify(dao).deleteByRoleIdAndUserId("role1", "u1")
        verify(eventPublisher).publishEvent(AuthRoleUserRelationsChanged("role1", listOf("u1")))
    }

    @Test
    fun bindTemporal_bothBoundsNull_skipsComparisonAndBinds() {
        // start==null OR end==null ⇒ the start/end comparison branch is skipped.
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1", "tenantA"))
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1")).thenReturn(emptyList())
        whenCalled(authRoleUserService.findSodViolationMessage(anyString(), anyString(), anyString())).thenReturn(null)
        whenCalled(dao.insert(anyRoleUser())).thenReturn("g")
        assertEquals("g", service.bindTemporal("role1", "u1", null, null))
    }

    @Test
    fun bindTemporal_existingTemporalGrant_isReplacedNotBlocked() {
        // An existing TEMPORAL grant (a bound is non-null) must NOT trigger the permanent-grant guard.
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1", "tenantA"))
        whenCalled(dao.searchByRoleIdAndUserId("role1", "u1"))
            .thenReturn(listOf(grant("old", "u1", LocalDateTime.now().minusDays(1), LocalDateTime.now())))
        whenCalled(authRoleUserService.findSodViolationMessage(anyString(), anyString(), anyString())).thenReturn(null)
        whenCalled(dao.insert(anyRoleUser())).thenReturn("g2")
        assertEquals("g2", service.bindTemporal("role1", "u1", null, null))
        verify(dao).deleteByRoleIdAndUserId("role1", "u1")
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
