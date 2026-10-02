package io.kudos.ability.security.enforcement.filter

import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

internal class AdminAuthorizationFilterTest {
    private fun filter(subject: Boolean, permitted: Boolean, registered: Boolean = true) = AdminAuthorizationFilter(
        { object : IAuthzDecisionProvider {
            override fun hasSubject() = subject
            override fun isPermitted(permissionCode: String, context: Map<String, Any?>) = permitted
        } },
        { object : IPermissionPointRegistry {
            override fun resolve(method: String, path: String) = if (registered) "sys:resource:read" else null
        } },
        { EnforcementProperties().apply { enabled = false; shadowMode = true; publicPaths = listOf("/**") } },
    )

    @Test
    fun disabledShadowAndPublicOverridesCannotOpenAdminEndpoints() {
        listOf(filter(false, true) to 401, filter(true, false) to 403, filter(true, true, false) to 403).forEach { (filter, expected) ->
            val response = MockHttpServletResponse()
            var reached = false
            filter.doFilter(MockHttpServletRequest("GET", "/api/admin/sys/resource/getDetail"), response) { _, _ -> reached = true }
            assertFalse(reached)
            assertEquals(expected, response.status)
        }
    }

    @Test
    fun authorizedAdminAndNonAdminRequestsReachTheirHandlers() {
        listOf("/api/admin/sys/resource/getDetail", "/api/public/auth/authentication/transactions").forEach { path ->
            var reached = false
            filter(true, true).doFilter(MockHttpServletRequest("GET", path), MockHttpServletResponse()) { _, _ -> reached = true }
            assertTrue(reached)
        }
    }

    @Test
    fun absentAuthorizationBackendFailsClosedOnlyForAdminPaths() {
        val filter = AdminAuthorizationFilter({ null }, { null }, { EnforcementProperties() })
        val response = MockHttpServletResponse()
        var reached = false
        filter.doFilter(MockHttpServletRequest("POST", "/api/admin/user/account/resetPassword"), response) { _, _ -> reached = true }
        assertFalse(reached)
        assertEquals(503, response.status)
        filter.doFilter(MockHttpServletRequest("GET", "/health"), MockHttpServletResponse()) { _, _ -> reached = true }
        assertTrue(reached)
    }

    @Test
    fun encodedAdminPathCannotEvadeTheMandatoryGate() {
        val response = MockHttpServletResponse()
        var reached = false
        filter(false, true).doFilter(MockHttpServletRequest("GET", "/api/%61dmin/user/account/getDetail"), response) { _, _ -> reached = true }
        assertFalse(reached)
        assertEquals(401, response.status)
    }
}
