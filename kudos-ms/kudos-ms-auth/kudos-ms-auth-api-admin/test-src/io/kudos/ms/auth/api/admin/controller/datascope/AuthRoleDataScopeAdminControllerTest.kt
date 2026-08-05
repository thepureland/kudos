package io.kudos.ms.auth.api.admin.controller.datascope

import io.kudos.ms.auth.common.datascope.vo.request.AuthRoleOrgBindRequest
import io.kudos.ms.auth.common.datascope.vo.response.DataScopeVo
import io.kudos.ms.auth.core.role.datascope.service.iservice.IAuthRoleDataScopeService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [AuthRoleDataScopeAdminController] (pure delegation; service mocked, no Spring container).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleDataScopeAdminControllerTest {

    private val service = mock(IAuthRoleDataScopeService::class.java)
    private val controller = AuthRoleDataScopeAdminController(service)

    @Test
    fun updateScope_delegatesWithScope() {
        whenCalled(service.updateScope("r1", "CUSTOM")).thenReturn(true)
        assertTrue(controller.updateScope("r1", "CUSTOM"))
        verify(service).updateScope("r1", "CUSTOM")
    }

    @Test
    fun updateScope_nullScopeFalseOutcome() {
        whenCalled(service.updateScope("r2", null)).thenReturn(false)
        assertFalse(controller.updateScope("r2", null))
        verify(service).updateScope("r2", null)
    }

    @Test
    fun getRoleOrgIds_delegatesAndPreservesOrder() {
        val ids = linkedSetOf("o1", "o2", "o3")
        whenCalled(service.getOrgIdsByRoleId("r1")).thenReturn(ids)
        val result = controller.getRoleOrgIds("r1")
        assertSame(ids, result)
        assertEquals(listOf("o1", "o2", "o3"), result.toList())
        verify(service).getOrgIdsByRoleId("r1")
    }

    @Test
    fun bindRoleOrgs_unpacksRequestFields() {
        val request = AuthRoleOrgBindRequest(roleId = "r1", orgIds = listOf("o1", "o2"))
        whenCalled(service.bindOrgs("r1", request.orgIds)).thenReturn(2)
        assertEquals(2, controller.bindRoleOrgs(request))
        verify(service).bindOrgs("r1", request.orgIds)
    }

    @Test
    fun bindRoleOrgs_emptyDefaultClearsAll() {
        val request = AuthRoleOrgBindRequest(roleId = "r1")
        assertTrue(request.orgIds.isEmpty())
        whenCalled(service.bindOrgs("r1", emptyList())).thenReturn(0)
        assertEquals(0, controller.bindRoleOrgs(request))
        verify(service).bindOrgs("r1", emptyList())
    }

    @Test
    fun resolve_delegates() {
        // orgIds is now a derived accessor over the dimension map; ofOrgs states the same intent.
        val vo = DataScopeVo.ofOrgs(setOf("o1"), self = true)
        whenCalled(service.resolveUserDataScope("u1")).thenReturn(vo)
        assertSame(vo, controller.resolve("u1"))
        verify(service).resolveUserDataScope("u1")
    }
}
