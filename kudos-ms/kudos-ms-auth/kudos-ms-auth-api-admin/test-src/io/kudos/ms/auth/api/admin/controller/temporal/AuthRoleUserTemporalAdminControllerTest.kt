package io.kudos.ms.auth.api.admin.controller.temporal

import io.kudos.ms.auth.common.temporal.vo.request.AuthRoleUserTemporalBindRequest
import io.kudos.ms.auth.common.temporal.vo.response.RoleTemporalGrantRow
import io.kudos.ms.auth.core.role.temporal.service.iservice.IAuthRoleUserTemporalService
import org.mockito.ArgumentMatchers
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import java.time.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import org.mockito.Mockito.`when` as whenCalled

/**
 * Test for [AuthRoleUserTemporalAdminController] (pure delegation; service mocked, no Spring container).
 *
 * `getGrantsByRoleId` has a Kotlin default `now` parameter, so the controller's 1-arg call resolves
 * (via the synthetic `$default`) to the 2-arg abstract method with a runtime `LocalDateTime.now()`.
 * It is stubbed/verified with null-safe matchers ([eqStr]/[anyTime]) to avoid Mockito returning a
 * `null` matcher for Kotlin non-null parameters.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthRoleUserTemporalAdminControllerTest {

    private val service = mock(IAuthRoleUserTemporalService::class.java)
    private val controller = AuthRoleUserTemporalAdminController(service)

    /** Null-safe [ArgumentMatchers.eq] for a non-null Kotlin String parameter. */
    private fun eqStr(value: String): String {
        ArgumentMatchers.eq(value)
        return value
    }

    /** Null-safe [ArgumentMatchers.any] for the non-null `now` parameter. */
    private fun anyTime(): LocalDateTime {
        ArgumentMatchers.any(LocalDateTime::class.java)
        return LocalDateTime.MIN
    }

    @Test
    fun getGrantsByRole_delegatesPreservingOrder() {
        val rows = listOf(
            RoleTemporalGrantRow(id = "g1", userId = "u1", active = true),
            RoleTemporalGrantRow(id = "g2", userId = "u2", active = false),
        )
        whenCalled(service.getGrantsByRoleId(eqStr("r1"), anyTime())).thenReturn(rows)
        val result = controller.getGrantsByRole("r1")
        assertSame(rows, result)
        assertEquals(listOf("g1", "g2"), result.map { it.id })
        verify(service).getGrantsByRoleId(eqStr("r1"), anyTime())
    }

    @Test
    fun getGrantsByRole_emptyResult() {
        whenCalled(service.getGrantsByRoleId(eqStr("none"), anyTime())).thenReturn(emptyList())
        assertEquals(emptyList(), controller.getGrantsByRole("none"))
    }

    @Test
    fun bind_unpacksAllFieldsWithWindow() {
        val start = LocalDateTime.of(2026, 1, 1, 0, 0)
        val end = LocalDateTime.of(2026, 12, 31, 23, 59)
        val request = AuthRoleUserTemporalBindRequest(roleId = "r1", userId = "u1", startTime = start, endTime = end)
        whenCalled(service.bindTemporal("r1", "u1", start, end)).thenReturn("grant-1")
        assertEquals("grant-1", controller.bind(request))
        verify(service).bindTemporal("r1", "u1", start, end)
    }

    @Test
    fun bind_nullWindowDefaults() {
        val request = AuthRoleUserTemporalBindRequest(roleId = "r2", userId = "u2")
        assertEquals(null, request.startTime)
        assertEquals(null, request.endTime)
        whenCalled(service.bindTemporal("r2", "u2", null, null)).thenReturn("grant-2")
        assertEquals("grant-2", controller.bind(request))
        verify(service).bindTemporal("r2", "u2", null, null)
    }

    @Test
    fun purgeExpired_delegatesCount() {
        whenCalled(service.purgeExpired()).thenReturn(7)
        assertEquals(7, controller.purgeExpired())
        verify(service).purgeExpired()
    }
}
