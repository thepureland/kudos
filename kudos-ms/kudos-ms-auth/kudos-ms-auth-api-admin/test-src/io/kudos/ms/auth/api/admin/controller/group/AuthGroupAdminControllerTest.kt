package io.kudos.ms.auth.api.admin.controller.group

import io.kudos.ms.auth.common.group.vo.request.AuthGroupBatchBindUsersRequest
import io.kudos.ms.auth.common.group.vo.response.GroupDeleteImpactVo
import io.kudos.ms.auth.common.role.vo.response.BatchBindResultVo
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupRoleService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupService
import io.kudos.ms.auth.core.group.service.iservice.IAuthGroupUserService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [AuthGroupAdminController] (pure delegation; services mocked with Mockito, no Spring container).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthGroupAdminControllerTest {

    private val service = mock(IAuthGroupService::class.java)
    private val userService = mock(IAuthGroupUserService::class.java)
    private val roleService = mock(IAuthGroupRoleService::class.java)
    private val controller = AuthGroupAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
        ReflectionTestUtils.setField(it, "authGroupUserService", userService)
        ReflectionTestUtils.setField(it, "authGroupRoleService", roleService)
    }

    @Test
    fun listUserIds_delegatesAndPreservesOrder() {
        val ids = linkedSetOf("u1", "u2")
        whenCalled(userService.getUserIdsByGroupId("g1")).thenReturn(ids)
        val result = controller.listUserIds("g1")
        assertSame(ids, result)
        assertEquals(listOf("u1", "u2"), result.toList())
        verify(userService).getUserIdsByGroupId("g1")
    }

    @Test
    fun listGroupIdsByUser_delegates() {
        val ids = setOf("g1", "g2")
        whenCalled(userService.getGroupIdsByUserId("u1")).thenReturn(ids)
        assertSame(ids, controller.listGroupIdsByUser("u1"))
        verify(userService).getGroupIdsByUserId("u1")
    }

    @Test
    fun bindUsers_delegatesCount() {
        val body = listOf("u1", "u2")
        whenCalled(userService.batchBind("g1", body)).thenReturn(2)
        assertEquals(2, controller.bindUsers("g1", body))
        verify(userService).batchBind("g1", body)
    }

    @Test
    fun unbindUser_delegatesBothOutcomes() {
        whenCalled(userService.unbind("g1", "u1")).thenReturn(true)
        whenCalled(userService.unbind("g1", "u2")).thenReturn(false)
        assertTrue(controller.unbindUser("g1", "u1"))
        assertFalse(controller.unbindUser("g1", "u2"))
    }

    @Test
    fun listRoleIds_delegates() {
        val ids = setOf("r1", "r2")
        whenCalled(roleService.getRoleIdsByGroupId("g1")).thenReturn(ids)
        assertSame(ids, controller.listRoleIds("g1"))
        verify(roleService).getRoleIdsByGroupId("g1")
    }

    @Test
    fun listGroupIdsByRole_delegates() {
        val ids = setOf("g1", "g2")
        whenCalled(roleService.getGroupIdsByRoleId("r1")).thenReturn(ids)
        assertSame(ids, controller.listGroupIdsByRole("r1"))
        verify(roleService).getGroupIdsByRoleId("r1")
    }

    @Test
    fun bindRoles_delegatesCount() {
        val body = listOf("r1")
        whenCalled(roleService.batchBind("g1", body)).thenReturn(1)
        assertEquals(1, controller.bindRoles("g1", body))
        verify(roleService).batchBind("g1", body)
    }

    @Test
    fun unbindRole_delegatesBothOutcomes() {
        whenCalled(roleService.unbind("g1", "r1")).thenReturn(true)
        whenCalled(roleService.unbind("g1", "r2")).thenReturn(false)
        assertTrue(controller.unbindRole("g1", "r1"))
        assertFalse(controller.unbindRole("g1", "r2"))
    }

    @Test
    fun getDeleteImpact_delegatesAndForwardsList() {
        val groupIds = listOf("g1", "g2")
        val vo = GroupDeleteImpactVo(users = 3, roles = 4)
        whenCalled(service.getDeleteImpact(groupIds)).thenReturn(vo)
        assertSame(vo, controller.getDeleteImpact(groupIds))
        verify(service).getDeleteImpact(groupIds)
    }

    @Test
    fun batchBindUsers_unpacksRequestFields() {
        val request = AuthGroupBatchBindUsersRequest(groupIds = listOf("g1"), userIds = listOf("u1", "u2"))
        val vo = BatchBindResultVo(ok = 2, failures = emptyList())
        whenCalled(service.batchBindUsers(request.groupIds, request.userIds)).thenReturn(vo)
        assertSame(vo, controller.batchBindUsers(request))
        verify(service).batchBindUsers(request.groupIds, request.userIds)
    }
}
