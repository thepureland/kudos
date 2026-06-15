package io.kudos.ms.auth.api.admin.controller.role

import io.kudos.ms.auth.common.role.vo.request.AuthRoleBatchBindUsersRequest
import io.kudos.ms.auth.common.role.vo.request.AuthRoleCopyRequest
import io.kudos.ms.auth.common.role.vo.response.BatchBindResultVo
import io.kudos.ms.auth.common.role.vo.response.EffectivePermissionsVo
import io.kudos.ms.auth.common.role.vo.response.RoleDeleteImpactVo
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleResourceService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleUserService
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
 * Test for [AuthRoleAdminController] (pure delegation; services mocked with Mockito, no Spring container).
 *
 * The base-controller `service` field and the two `@Resource` collaborators are injected via reflection.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleAdminControllerTest {

    private val service = mock(IAuthRoleService::class.java)
    private val userService = mock(IAuthRoleUserService::class.java)
    private val resourceService = mock(IAuthRoleResourceService::class.java)
    private val controller = AuthRoleAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
        ReflectionTestUtils.setField(it, "authRoleUserService", userService)
        ReflectionTestUtils.setField(it, "authRoleResourceService", resourceService)
    }

    @Test
    fun updateActive_delegatesBothBooleans() {
        whenCalled(service.updateActive("r1", true)).thenReturn(true)
        whenCalled(service.updateActive("r2", false)).thenReturn(false)

        assertTrue(controller.updateActive("r1", true))
        assertFalse(controller.updateActive("r2", false))
        verify(service).updateActive("r1", true)
        verify(service).updateActive("r2", false)
    }

    @Test
    fun listUserIds_delegatesAndPreservesOrder() {
        val ids = linkedSetOf("u1", "u2", "u3")
        whenCalled(userService.getUserIdsByRoleId("r1")).thenReturn(ids)

        val result = controller.listUserIds("r1")
        assertSame(ids, result)
        assertEquals(listOf("u1", "u2", "u3"), result.toList())
        verify(userService).getUserIdsByRoleId("r1")
    }

    @Test
    fun listUserIds_emptyResult() {
        whenCalled(userService.getUserIdsByRoleId("none")).thenReturn(emptySet())
        assertTrue(controller.listUserIds("none").isEmpty())
    }

    @Test
    fun listRoleIdsByUser_delegates() {
        val ids = setOf("r1", "r2")
        whenCalled(userService.getRoleIdsByUserId("u1")).thenReturn(ids)
        assertSame(ids, controller.listRoleIdsByUser("u1"))
        verify(userService).getRoleIdsByUserId("u1")
    }

    @Test
    fun bindUsers_delegatesCountAndForwardsBody() {
        val body = listOf("u1", "u2")
        whenCalled(userService.batchBind("r1", body)).thenReturn(2)
        assertEquals(2, controller.bindUsers("r1", body))
        verify(userService).batchBind("r1", body)
    }

    @Test
    fun bindUsers_emptyBodyReturnsZero() {
        whenCalled(userService.batchBind("r1", emptyList())).thenReturn(0)
        assertEquals(0, controller.bindUsers("r1", emptyList()))
    }

    @Test
    fun unbindUser_delegatesBothOutcomes() {
        whenCalled(userService.unbind("r1", "u1")).thenReturn(true)
        whenCalled(userService.unbind("r1", "u2")).thenReturn(false)
        assertTrue(controller.unbindUser("r1", "u1"))
        assertFalse(controller.unbindUser("r1", "u2"))
        verify(userService).unbind("r1", "u1")
        verify(userService).unbind("r1", "u2")
    }

    @Test
    fun listResourceIds_delegates() {
        val ids = setOf("res1", "res2")
        whenCalled(resourceService.getResourceIdsByRoleId("r1")).thenReturn(ids)
        assertSame(ids, controller.listResourceIds("r1"))
        verify(resourceService).getResourceIdsByRoleId("r1")
    }

    @Test
    fun listRoleIdsByResource_delegates() {
        val ids = setOf("r1", "r2")
        whenCalled(resourceService.getRoleIdsByResourceId("res1")).thenReturn(ids)
        assertSame(ids, controller.listRoleIdsByResource("res1"))
        verify(resourceService).getRoleIdsByResourceId("res1")
    }

    @Test
    fun bindResources_delegates() {
        val body = listOf("res1", "res2", "res3")
        whenCalled(resourceService.batchBind("r1", body)).thenReturn(3)
        assertEquals(3, controller.bindResources("r1", body))
        verify(resourceService).batchBind("r1", body)
    }

    @Test
    fun unbindResource_delegatesBothOutcomes() {
        whenCalled(resourceService.unbind("r1", "res1")).thenReturn(true)
        whenCalled(resourceService.unbind("r1", "res2")).thenReturn(false)
        assertTrue(controller.unbindResource("r1", "res1"))
        assertFalse(controller.unbindResource("r1", "res2"))
    }

    @Test
    fun getEffectivePermissions_delegates() {
        val vo = EffectivePermissionsVo(emptyList(), emptyList(), emptyMap(), emptyMap())
        whenCalled(service.getEffectivePermissions("u1")).thenReturn(vo)
        assertSame(vo, controller.getEffectivePermissions("u1"))
        verify(service).getEffectivePermissions("u1")
    }

    @Test
    fun getDeleteImpact_delegatesAndForwardsList() {
        val roleIds = listOf("r1", "r2")
        val vo = RoleDeleteImpactVo(users = 5, groups = 2)
        whenCalled(service.getDeleteImpact(roleIds)).thenReturn(vo)
        assertSame(vo, controller.getDeleteImpact(roleIds))
        verify(service).getDeleteImpact(roleIds)
    }

    @Test
    fun batchBindUsers_unpacksRequestFields() {
        val request = AuthRoleBatchBindUsersRequest(roleIds = listOf("r1", "r2"), userIds = listOf("u1"))
        val vo = BatchBindResultVo(ok = 2, failures = emptyList())
        whenCalled(service.batchBindUsers(request.roleIds, request.userIds)).thenReturn(vo)
        assertSame(vo, controller.batchBindUsers(request))
        verify(service).batchBindUsers(request.roleIds, request.userIds)
    }

    @Test
    fun copyRole_unpacksAllFour() {
        val request = AuthRoleCopyRequest(sourceId = "src", code = "C1", name = "Name", copyResources = false)
        whenCalled(service.copyRole("src", "C1", "Name", false)).thenReturn("new-id")
        assertEquals("new-id", controller.copyRole(request))
        verify(service).copyRole("src", "C1", "Name", false)
    }

    @Test
    fun copyRole_defaultCopyResourcesTrue() {
        val request = AuthRoleCopyRequest(sourceId = "src", code = "C1", name = "Name")
        assertTrue(request.copyResources)
        whenCalled(service.copyRole("src", "C1", "Name", true)).thenReturn("id2")
        assertEquals("id2", controller.copyRole(request))
        verify(service).copyRole("src", "C1", "Name", true)
    }
}
