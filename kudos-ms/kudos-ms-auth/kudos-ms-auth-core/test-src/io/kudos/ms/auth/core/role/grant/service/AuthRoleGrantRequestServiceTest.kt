package io.kudos.ms.auth.core.role.grant.service

import io.kudos.ms.auth.common.grant.enums.GrantRequestStatus
import io.kudos.ms.auth.core.role.grant.service.iservice.IAuthRoleGrantRequestService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
import io.kudos.test.container.annotations.EnabledIfDockerInstalled
import io.kudos.test.rdb.RdbAndRedisCacheTestBase
import jakarta.annotation.Resource
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

    @Test
    fun submit_createsPendingRequest() {
        val id = service.submit(approvalRoleId, user2, "need access for project X")
        val req = service.get(id)
        assertEquals(GrantRequestStatus.PENDING.name, req!!.status)
        assertEquals(approvalRoleId, req.roleId)
        assertEquals(user2, req.userId)
        assertEquals("svc-tenant-grant-1", req.tenantId, "tenant is derived from the role")
    }

    @Test
    fun submit_whenUserAlreadyHoldsRole_rejected() {
        val err = assertFailsWith<IllegalArgumentException> { service.submit(normalRoleId, user1, null) }
        assertTrue(err.message!!.contains("already holds"))
    }

    @Test
    fun submit_duplicatePending_rejected() {
        service.submit(approvalRoleId, user2, "first")
        val err = assertFailsWith<IllegalArgumentException> { service.submit(approvalRoleId, user2, "second") }
        assertTrue(err.message!!.contains("pending"))
    }

    @Test
    fun approve_bindsUserAndMarksApproved() {
        val id = service.submit(approvalRoleId, user2, null)
        assertFalse(authRoleUserService.exists(approvalRoleId, user2), "not bound before approval")

        assertTrue(service.approve(id, "looks good"))

        assertTrue(authRoleUserService.exists(approvalRoleId, user2), "bound after approval")
        val req = service.get(id)!!
        assertEquals(GrantRequestStatus.APPROVED.name, req.status)
        assertEquals("looks good", req.decisionComment)
    }

    @Test
    fun reject_doesNotBind() {
        val id = service.submit(approvalRoleId, user2, null)
        assertTrue(service.reject(id, "denied"))
        assertFalse(authRoleUserService.exists(approvalRoleId, user2), "must NOT be bound after reject")
        assertEquals(GrantRequestStatus.REJECTED.name, service.get(id)!!.status)
    }

    @Test
    fun cancel_marksCancelled() {
        val id = service.submit(approvalRoleId, user2, null)
        assertTrue(service.cancel(id))
        assertEquals(GrantRequestStatus.CANCELLED.name, service.get(id)!!.status)
    }

    @Test
    fun approve_nonPending_rejected() {
        val id = service.submit(approvalRoleId, user2, null)
        service.reject(id, "no")
        val err = assertFailsWith<IllegalArgumentException> { service.approve(id, "too late") }
        assertTrue(err.message!!.contains("already"))
    }
}
