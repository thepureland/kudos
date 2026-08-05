package io.kudos.ms.auth.api.admin.controller.role

import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionViolationVo
import io.kudos.ms.auth.core.role.exclusion.service.iservice.IAuthRoleExclusionService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.springframework.test.util.ReflectionTestUtils
import kotlin.test.Test
import kotlin.test.assertSame
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [AuthRoleExclusionAdminController] (pure delegation; service mocked, no Spring container).
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleExclusionAdminControllerTest {

    private val service = mock(IAuthRoleExclusionService::class.java)
    private val controller = AuthRoleExclusionAdminController().also {
        ReflectionTestUtils.setField(it, "service", service)
    }

    @Test
    fun findViolations_delegates() {
        val vo = AuthRoleExclusionViolationVo(
            exclusionId = "ex1",
            roleAId = "rA",
            roleBId = "rB",
            violatingUserIds = listOf("u1", "u2"),
        )
        whenCalled(service.findViolatingUserIds("ex1")).thenReturn(vo)
        assertSame(vo, controller.findViolations("ex1"))
        verify(service).findViolatingUserIds("ex1")
    }

    @Test
    fun findViolations_emptyViolations() {
        val vo = AuthRoleExclusionViolationVo(
            exclusionId = "ex2",
            roleAId = "rA",
            roleBId = "rB",
            violatingUserIds = emptyList(),
        )
        whenCalled(service.findViolatingUserIds("ex2")).thenReturn(vo)
        assertSame(vo, controller.findViolations("ex2"))
        verify(service).findViolatingUserIds("ex2")
    }
}
