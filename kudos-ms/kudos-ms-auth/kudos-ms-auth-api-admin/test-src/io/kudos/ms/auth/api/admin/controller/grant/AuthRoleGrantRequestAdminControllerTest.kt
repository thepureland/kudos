package io.kudos.ms.auth.api.admin.controller.grant

import io.kudos.ms.auth.common.grant.vo.request.AuthRoleGrantDecisionRequest
import io.kudos.ms.auth.common.grant.vo.request.AuthRoleGrantSubmitRequest
import io.kudos.ms.auth.core.role.grant.service.iservice.IAuthRoleGrantRequestService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [AuthRoleGrantRequestAdminController] (pure delegation; service mocked, no Spring container).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleGrantRequestAdminControllerTest {

    private val service = mock(IAuthRoleGrantRequestService::class.java)
    private val controller = AuthRoleGrantRequestAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun submit_unpacksFieldsWithReason() {
        val request = AuthRoleGrantSubmitRequest(roleId = "r1", userId = "u1", reason = "need it")
        whenCalled(service.submit("r1", "u1", "need it")).thenReturn("req-1")
        assertEquals("req-1", controller.submit(request))
        verify(service).submit("r1", "u1", "need it")
    }

    @Test
    fun submit_nullReasonDefault() {
        val request = AuthRoleGrantSubmitRequest(roleId = "r2", userId = "u2")
        assertEquals(null, request.reason)
        whenCalled(service.submit("r2", "u2", null)).thenReturn("req-2")
        assertEquals("req-2", controller.submit(request))
        verify(service).submit("r2", "u2", null)
    }

    @Test
    fun approve_delegatesWithComment() {
        val request = AuthRoleGrantDecisionRequest(id = "req-1", comment = "ok")
        whenCalled(service.approve("req-1", "ok")).thenReturn(true)
        assertTrue(controller.approve(request))
        verify(service).approve("req-1", "ok")
    }

    @Test
    fun approve_falseOutcomeNullComment() {
        val request = AuthRoleGrantDecisionRequest(id = "req-3")
        assertEquals(null, request.comment)
        whenCalled(service.approve("req-3", null)).thenReturn(false)
        assertFalse(controller.approve(request))
        verify(service).approve("req-3", null)
    }

    @Test
    fun reject_delegates() {
        val request = AuthRoleGrantDecisionRequest(id = "req-1", comment = "no")
        whenCalled(service.reject("req-1", "no")).thenReturn(true)
        assertTrue(controller.reject(request))
        verify(service).reject("req-1", "no")
    }

    @Test
    fun cancel_usesIdOnlyIgnoringComment() {
        val request = AuthRoleGrantDecisionRequest(id = "req-1", comment = "ignored")
        whenCalled(service.cancel("req-1")).thenReturn(true)
        assertTrue(controller.cancel(request))
        verify(service).cancel("req-1")
    }
}
