package io.kudos.ms.auth.api.admin.controller.resource

import io.kudos.ms.auth.core.role.service.iservice.IAuthRoleService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [AuthResourcePermissionAdminController] (pure delegation; service mocked, no Spring container).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthResourcePermissionAdminControllerTest {

    private val authRoleService = mock(IAuthRoleService::class.java)
    private val controller = AuthResourcePermissionAdminController(authRoleService)

    @Test
    fun roleNamesByResourceIds_delegatesAndForwardsList() {
        val resourceIds = listOf("res1", "res2")
        val map = mapOf("res1" to listOf("Admin", "Editor"))
        whenCalled(authRoleService.getRoleNamesByResourceIds(resourceIds)).thenReturn(map)
        val result = controller.roleNamesByResourceIds(resourceIds)
        assertSame(map, result)
        assertEquals(listOf("Admin", "Editor"), result["res1"])
        verify(authRoleService).getRoleNamesByResourceIds(resourceIds)
    }

    @Test
    fun roleNamesByResourceIds_emptyResult() {
        val resourceIds = listOf("resX")
        whenCalled(authRoleService.getRoleNamesByResourceIds(resourceIds)).thenReturn(emptyMap())
        assertTrue(controller.roleNamesByResourceIds(resourceIds).isEmpty())
        verify(authRoleService).getRoleNamesByResourceIds(resourceIds)
    }
}
