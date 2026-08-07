package io.kudos.ms.auth.core.role.grant.service

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.grant.enums.GrantRequestStatus
import io.kudos.ms.auth.core.role.grant.service.iservice.IAuthRoleGrantRequestService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * junit test for AuthRoleGrantRequestService — the role-grant approval workflow.
 *
 * Test data source: `AuthRoleGrantRequestServiceTest.sql`
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@EnabledIfDockerInstalled
class AuthRoleGrantRequestServiceTest : RdbAndRedisCacheTestBase() {

    @Resource
    private lateinit var service: IAuthRoleGrantRequestService

    @Resource
    private lateinit var authRoleUserService: IAuthRoleUserService

    private val normalRoleId = "grant000-0000-0000-0000-000000000010"
    private val approvalRoleId = "grant000-0000-0000-0000-000000000011"
    private val user1 = "grant000-0000-0000-0000-000000000001" // already holds normalRole
    private val user2 = "grant000-0000-0000-0000-000000000002"
    private val approver = "grant000-0000-0000-0000-000000000003"
    private val bystander = "grant000-0000-0000-0000-000000000004"

    /** Binds a logged-in principal to the thread, the way the web filter does for a real request. */
    private fun loginAs(userId: String) {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal(id = userId, tenantId = "svc-tenant-grant-1", username = userId)
            },
        )
    }

    @AfterTest
    fun clearContext() = KudosContextHolder.clear()

    private fun submitApprovalRequest(reason: String? = null): String {
        loginAs(user1)
        return service.submit(approvalRoleId, user2, reason)
    }

    @Test
    fun submit_createsPendingRequest() {
        val id = submitApprovalRequest("need access for project X")
        val req = service.get(id)
        assertEquals(GrantRequestStatus.PENDING.name, req!!.status)
        assertEquals(approvalRoleId, req.roleId)
        assertEquals(user2, req.userId)
        assertEquals("svc-tenant-grant-1", req.tenantId, "tenant is derived from the role")
    }

    @Test
    fun submit_whenUserAlreadyHoldsRole_rejected() {
        loginAs(user1)
        val err = assertFailsWith<IllegalArgumentException> { service.submit(normalRoleId, user1, null) }
        assertTrue(err.message!!.contains("already holds"))
    }

    @Test
    fun submit_duplicatePending_rejected() {
        submitApprovalRequest("first")
        val err = assertFailsWith<IllegalArgumentException> { service.submit(approvalRoleId, user2, "second") }
        assertTrue(err.message!!.contains("pending"))
    }

    @Test
    fun approve_bindsUserAndMarksApproved() {
        val id = submitApprovalRequest()
        loginAs(approver)
        assertFalse(authRoleUserService.exists(approvalRoleId, user2), "not bound before approval")

        assertTrue(service.approve(id, "looks good"))

        assertTrue(authRoleUserService.exists(approvalRoleId, user2), "bound after approval")
        val req = service.get(id)!!
        assertEquals(GrantRequestStatus.APPROVED.name, req.status)
        assertEquals("looks good", req.decisionComment)
    }

    @Test
    fun reject_doesNotBind() {
        val id = submitApprovalRequest()
        loginAs(approver)
        assertTrue(service.reject(id, "denied"))
        assertFalse(authRoleUserService.exists(approvalRoleId, user2), "must NOT be bound after reject")
        assertEquals(GrantRequestStatus.REJECTED.name, service.get(id)!!.status)
    }

    @Test
    fun cancel_marksCancelled() {
        val id = submitApprovalRequest()
        assertTrue(service.cancel(id))
        assertEquals(GrantRequestStatus.CANCELLED.name, service.get(id)!!.status)
    }

    @Test
    fun approve_nonPending_rejected() {
        val id = submitApprovalRequest()
        loginAs(approver)
        service.reject(id, "no")
        loginAs(approver)
        val err = assertFailsWith<IllegalArgumentException> { service.approve(id, "too late") }
        assertTrue(err.message!!.contains("already"))
    }

    // -------------------- approver eligibility --------------------

    @Test
    fun approve_byAUserWhoCouldNotHaveGrantedIt_rejected() {
        // Without this check the workflow is theatre: any authenticated user could sign off, which
        // adds a row to a table and nothing to the security of the system.
        val id = submitApprovalRequest()
        loginAs(bystander)
        val err = assertFailsWith<IllegalArgumentException> { service.approve(id, "rubber stamp") }
        assertTrue(err.message!!.contains("may not decide requests"), err.message!!)
        assertFalse(authRoleUserService.exists(approvalRoleId, user2), "and nothing was bound")
        assertEquals(GrantRequestStatus.PENDING.name, service.get(id)!!.status, "the request stays open")
    }

    @Test
    fun approve_withoutALoggedInApprover_rejected() {
        val id = submitApprovalRequest()
        KudosContextHolder.clear()
        val err = assertFailsWith<IllegalArgumentException> { service.approve(id, "anonymous") }
        assertTrue(err.message!!.contains("attributable"), err.message!!)
    }

    @Test
    fun approve_recordsWhoDecided() {
        val id = submitApprovalRequest()
        loginAs(approver)
        service.approve(id, "ok")
        assertEquals(approver, service.get(id)!!.approverId)
    }

    // -------------------- the approval requirement itself --------------------

    @Test
    fun anApprovalRequiredRoleCannotBeAssignedByADirectBind() {
        // The flag has to bite on every write path, otherwise the workflow is optional in practice:
        // whoever finds the plain bind endpoint simply skips it.
        val err = assertFailsWith<IllegalArgumentException> {
            authRoleUserService.batchBind(approvalRoleId, listOf(user2))
        }
        assertTrue(err.message!!.contains("requires approval"), err.message!!)
    }

    @Test
    fun aRoleWithoutTheFlagIsUnaffected() {
        assertEquals(1, authRoleUserService.batchBind(normalRoleId, listOf(user2)))
    }
}
