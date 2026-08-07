package io.kudos.ms.auth.core.role.grant.service

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.grant.enums.GrantRequestStatus
import io.kudos.ms.auth.core.role.dao.AuthRoleDao
import io.kudos.ms.auth.core.role.grant.dao.AuthRoleGrantRequestDao
import io.kudos.ms.auth.core.role.grant.model.po.AuthRoleGrantRequest
import io.kudos.ms.auth.core.role.grant.service.impl.AuthRoleGrantRequestService
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.ArgumentMatchers
import org.mockito.ArgumentMatchers.anyString
import io.kudos.ms.auth.core.role.dao.AuthRoleUserDao
import io.kudos.ms.auth.core.platform.authz.PlatformAdministratorPolicy
import io.kudos.ms.auth.core.policy.TenantAdministrationGuard
import io.kudos.ms.auth.core.version.dao.AuthPrincipalVersionDao
import io.kudos.ms.auth.core.role.model.po.AuthRoleUser
import org.mockito.Mockito.mock
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Pure-logic unit test for [AuthRoleGrantRequestService] (collaborators mocked with Mockito; no
 * Spring container, no DB) — complements the container-backed [AuthRoleGrantRequestServiceTest]
 * by exercising the branches that the integration fixture cannot reach deterministically:
 *
 *  - submit() blank-arg guards (roleId / userId) and the role-not-found guard;
 *  - submit() requester-id capture from the thread-bound login context;
 *  - approve() self-approval guard (requester == approver) using a faked login context, and the
 *    branch where the guard is skipped because no user is bound;
 *  - approve() ordering: the bind is performed before the status flip (bind failure ⇒ no update);
 *  - the loadPendingOrThrow guard for missing / non-PENDING rows on approve/reject/cancel.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AuthRoleGrantRequestServiceUnitTest {

    private val dao = mock(AuthRoleGrantRequestDao::class.java)
    private val authRoleDao = mock(AuthRoleDao::class.java)
    private val authRoleUserDao = mock(AuthRoleUserDao::class.java)
    private val platformAdministratorPolicy = mock(PlatformAdministratorPolicy::class.java)
    private val tenantAdministrationGuard = mock(TenantAdministrationGuard::class.java)
    private val authPrincipalVersionDao = mock(AuthPrincipalVersionDao::class.java)
    private val authRoleUserService = mock(IAuthRoleUserService::class.java)

    private val service = AuthRoleGrantRequestService(dao).apply {
        inject("authRoleDao", authRoleDao)
        inject("authRoleUserService", authRoleUserService)
        // Approving requires the authority to grant the role (see assertMayApprove).
        inject("authRoleUserDao", authRoleUserDao)
        inject("platformAdministratorPolicy", platformAdministratorPolicy)
        inject("tenantAdministrationGuard", tenantAdministrationGuard)
        inject("authPrincipalVersionDao", authPrincipalVersionDao)
    }

    private fun AuthRoleGrantRequestService.inject(field: String, value: Any) {
        val f = AuthRoleGrantRequestService::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }

    // Kotlin non-null params reject Mockito's null-returning matchers; coalesce to a real value.
    private fun anyRequest(): AuthRoleGrantRequest =
        ArgumentMatchers.any(AuthRoleGrantRequest::class.java) ?: AuthRoleGrantRequest { id = "x" }

    @Suppress("UNCHECKED_CAST")
    private fun anyStringList(): List<String> =
        (ArgumentMatchers.any(List::class.java) as List<String>?) ?: emptyList()

    private fun captureRequest(captor: org.mockito.ArgumentCaptor<AuthRoleGrantRequest>): AuthRoleGrantRequest =
        captor.capture() ?: AuthRoleGrantRequest { id = "x" }

    @AfterTest
    fun cleanup() {
        // Each test that binds a login context must not leak it into the next.
        KudosContextHolder.clear()
    }

    private fun login(userId: String) {
        val ctx = KudosContext()
        ctx.user = SessionUserPrincipal(id = userId, tenantId = "t1", username = "u_$userId")
        KudosContextHolder.set(ctx)
    }

    private fun role(id: String, tenant: String = "t1") = AuthRole {
        this.id = id
        this.tenantId = tenant
        this.active = true
    }

    private fun pending(id: String, requester: String? = null) = AuthRoleGrantRequest {
        this.id = id
        this.roleId = "role1"
        this.userId = "user1"
        this.tenantId = "t1"
        this.status = GrantRequestStatus.PENDING.name
        this.requesterId = requester
    }

    // ---------------------------------------------------------------- submit guards

    @Test
    fun submit_blankRoleId_rejected() {
        val err = assertFailsWith<IllegalArgumentException> { service.submit("  ", "user1", null) }
        assertTrue(err.message!!.contains("roleId must not be blank"))
    }

    @Test
    fun submit_blankUserId_rejected() {
        val err = assertFailsWith<IllegalArgumentException> { service.submit("role1", "", null) }
        assertTrue(err.message!!.contains("userId must not be blank"))
    }

    @Test
    fun submit_roleNotFound_rejected() {
        whenCalled(authRoleDao.get("ghost")).thenReturn(null)
        val err = assertFailsWith<IllegalArgumentException> { service.submit("ghost", "user1", null) }
        assertTrue(err.message!!.contains("Role not found: ghost"))
    }

    @Test
    fun submit_happyPath_capturesRequesterFromContext() {
        login("approver-x")
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1", "tenantZ"))
        whenCalled(authRoleUserService.exists("role1", "user1")).thenReturn(false)
        whenCalled(dao.findPending("role1", "user1")).thenReturn(null)
        whenCalled(dao.insert(anyRequest())).thenReturn("new-req")

        val id = service.submit("role1", "user1", "reason")
        assertEquals("new-req", id)
        // Capture the inserted PO and verify tenant came from the role and requester from context.
        val captor = org.mockito.ArgumentCaptor.forClass(AuthRoleGrantRequest::class.java)
        verify(dao).insert(captureRequest(captor))
        val saved = captor.value
        assertEquals("tenantZ", saved.tenantId)
        assertEquals("approver-x", saved.requesterId)
        assertEquals(GrantRequestStatus.PENDING.name, saved.status)
    }

    @Test
    fun submit_userAlreadyHoldsRole_rejected() {
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1"))
        whenCalled(authRoleUserService.exists("role1", "user1")).thenReturn(true)
        val err = assertFailsWith<IllegalArgumentException> { service.submit("role1", "user1", null) }
        assertTrue(err.message!!.contains("already holds"))
    }

    @Test
    fun submit_duplicatePending_rejected() {
        whenCalled(authRoleDao.get("role1")).thenReturn(role("role1"))
        whenCalled(authRoleUserService.exists("role1", "user1")).thenReturn(false)
        whenCalled(dao.findPending("role1", "user1")).thenReturn(pending("dup"))
        val err = assertFailsWith<IllegalArgumentException> { service.submit("role1", "user1", null) }
        assertTrue(err.message!!.contains("already exists"))
    }

    // ---------------------------------------------------------------- approve: self-approval guard

    @Test
    fun approve_byOwnRequester_rejected() {
        login("self")
        whenCalled(dao.get("req1")).thenReturn(pending("req1", requester = "self"))
        val err = assertFailsWith<IllegalArgumentException> { service.approve("req1", "ok") }
        assertTrue(err.message!!.contains("cannot be approved by its own requester"))
        // The bind must NOT have been attempted.
        verify(authRoleUserService, never()).bindApproved(anyString(), anyString())
    }

    @Test
    fun approve_byDifferentUser_succeeds() {
        login("other-approver")
        whenCalled(dao.get("req2")).thenReturn(pending("req2", requester = "the-requester"))
        whenCalled(dao.transitionIfPending(anyRequest())).thenReturn(true)
        // Approving requires the authority to hand the role out: a live direct grant that is still
        // delegable. Being merely logged in and not the requester is not enough.
        grantsApprovalAuthority("other-approver", "role1")

        assertTrue(service.approve("req2", "approved"))
        verify(authRoleUserService).bindApproved("role1", "user1")
        val captor = org.mockito.ArgumentCaptor.forClass(AuthRoleGrantRequest::class.java)
        verify(dao).transitionIfPending(captureRequest(captor))
        assertEquals(GrantRequestStatus.APPROVED.name, captor.value.status)
        assertEquals("other-approver", captor.value.approverId)
        assertEquals("approved", captor.value.decisionComment)
    }

    @Test
    fun approve_noContextBound_isRefused() {
        // No login → no attributable approver. An approval workflow exists to record *who* accepted
        // the risk, so an anonymous approval is refused rather than treated as "guard cannot decide".
        // The only approval entry point is the REST endpoint, which always has a logged-in caller;
        // a batch context that needs to approve should authenticate as a service principal.
        whenCalled(dao.get("req3")).thenReturn(pending("req3", requester = "someone"))
        whenCalled(dao.transitionIfPending(anyRequest())).thenReturn(true)
        assertFailsWith<IllegalArgumentException> { service.approve("req3", null) }
        verify(authRoleUserService, never()).bindApproved(anyString(), anyString())
    }

    @Test
    fun approve_requesterUnknown_stillRequiresApprovalAuthority() {
        // A null requesterId only stands the *self-approval* guard down — it cannot excuse the
        // authority check, which asks whether this approver may hand out this role at all.
        login("approver")
        whenCalled(dao.get("req4")).thenReturn(pending("req4", requester = null))
        whenCalled(dao.transitionIfPending(anyRequest())).thenReturn(true)
        assertFailsWith<IllegalArgumentException> { service.approve("req4", null) }
        verify(authRoleUserService, never()).bindApproved(anyString(), anyString())
    }

    @Test
    fun approve_bindThrows_propagatesAndDoesNotUpdate() {
        login("approver")
        whenCalled(dao.get("req5")).thenReturn(pending("req5", requester = "someone"))
        grantsApprovalAuthority("approver", "role1")
        whenCalled(authRoleUserService.bindApproved(anyString(), anyString()))
            .thenThrow(IllegalArgumentException("SoD constraint violation"))
        assertFailsWith<IllegalArgumentException> { service.approve("req5", null) }
        // Status flip happens only after a successful bind, so update must never run.
        verify(dao, never()).transitionIfPending(anyRequest())
    }

    // ---------------------------------------------------------------- loadPendingOrThrow guard

    @Test
    fun approve_missingRequest_rejected() {
        whenCalled(dao.get("none")).thenReturn(null)
        val err = assertFailsWith<IllegalArgumentException> { service.approve("none", null) }
        assertTrue(err.message!!.contains("Grant request not found: none"))
    }

    @Test
    fun reject_nonPending_rejected() {
        val terminal = pending("t").apply { status = GrantRequestStatus.APPROVED.name }
        whenCalled(dao.get("t")).thenReturn(terminal)
        val err = assertFailsWith<IllegalArgumentException> { service.reject("t", null) }
        assertTrue(err.message!!.contains("already APPROVED"))
    }

    @Test
    fun reject_pending_flipsToRejectedNoBind() {
        login("rejector")
        whenCalled(dao.get("r")).thenReturn(pending("r"))
        grantsApprovalAuthority("rejector", "role1")
        whenCalled(dao.transitionIfPending(anyRequest())).thenReturn(true)
        assertTrue(service.reject("r", "no good"))
        val captor = org.mockito.ArgumentCaptor.forClass(AuthRoleGrantRequest::class.java)
        verify(dao).transitionIfPending(captureRequest(captor))
        assertEquals(GrantRequestStatus.REJECTED.name, captor.value.status)
        assertEquals("rejector", captor.value.approverId)
        verify(authRoleUserService, never()).bindApproved(anyString(), anyString())
    }

    @Test
    fun reject_byRequester_isRefused() {
        login("requester")
        whenCalled(dao.get("self-reject")).thenReturn(pending("self-reject", requester = "requester"))
        assertFailsWith<IllegalArgumentException> { service.reject("self-reject", "trying to bypass cancel") }
        verify(dao, never()).transitionIfPending(anyRequest())
    }

    @Test
    fun cancel_pending_flipsToCancelled() {
        login("requester")
        whenCalled(dao.get("c")).thenReturn(pending("c", requester = "requester"))
        whenCalled(dao.transitionIfPending(anyRequest())).thenReturn(true)
        assertTrue(service.cancel("c"))
        val captor = org.mockito.ArgumentCaptor.forClass(AuthRoleGrantRequest::class.java)
        verify(dao).transitionIfPending(captureRequest(captor))
        assertEquals(GrantRequestStatus.CANCELLED.name, captor.value.status)
    }

    @Test
    fun cancel_bySomeoneOtherThanRequester_isRefused() {
        login("intruder")
        whenCalled(dao.get("foreign")).thenReturn(pending("foreign", requester = "requester"))
        assertFailsWith<IllegalArgumentException> { service.cancel("foreign") }
        verify(dao, never()).transitionIfPending(anyRequest())
    }

    @Test
    fun concurrentDecisionThatLostTheAtomicTransitionRollsBackLoudly() {
        login("approver")
        whenCalled(dao.get("race")).thenReturn(pending("race", requester = "requester"))
        grantsApprovalAuthority("approver", "role1")
        whenCalled(dao.transitionIfPending(anyRequest())).thenReturn(false)

        assertFailsWith<IllegalStateException> { service.approve("race", "approved") }
    }

    @Test
    fun cancel_nonPending_rejected() {
        val terminal = pending("x").apply { status = GrantRequestStatus.CANCELLED.name }
        whenCalled(dao.get("x")).thenReturn(terminal)
        assertFalse(GrantRequestStatus.fromString(terminal.status) == GrantRequestStatus.PENDING)
        val err = assertFailsWith<IllegalArgumentException> { service.cancel("x") }
        assertTrue(err.message!!.contains("already CANCELLED"))
    }

    /** Stubs [approver] as holding [roleId] through a live, still-delegable direct grant. */
    private fun grantsApprovalAuthority(approver: String, roleId: String) {
        whenCalled(authRoleUserDao.findLiveDirectGrant(anyString(), anyString(), anyInstant()))
            .thenReturn(AuthRoleUser {
                this.roleId = roleId
                this.userId = approver
                this.delegableDepth = 1
                this.revoked = false
            })
    }

    /** Matcher for the non-null `now` parameter; Mockito returns null, Kotlin will not take it. */
    private fun anyInstant(): java.time.LocalDateTime =
        org.mockito.ArgumentMatchers.any(java.time.LocalDateTime::class.java) ?: java.time.LocalDateTime.now()
}
