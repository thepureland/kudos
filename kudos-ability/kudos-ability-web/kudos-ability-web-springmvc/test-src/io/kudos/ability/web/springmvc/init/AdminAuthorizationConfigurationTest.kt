package io.kudos.ability.web.springmvc.init

import io.kudos.ability.security.enforcement.filter.AdminAuthorizationFilter
import org.springframework.boot.web.servlet.FilterRegistrationBean
import org.springframework.context.annotation.AnnotationConfigApplicationContext
import org.springframework.core.Ordered
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

internal class AdminAuthorizationConfigurationTest {
    @Test
    fun standaloneMvcRegistersFailClosedAdminGateWithoutAuthModule() {
        AnnotationConfigApplicationContext(AdminAuthorizationConfiguration::class.java).use { context ->
            val registration = context.getBean("adminAuthorizationFilterRegistration", FilterRegistrationBean::class.java)
            val filter = assertIs<AdminAuthorizationFilter>(registration.filter)
            assertTrue(registration.isEnabled)
            assertTrue("/*" in registration.urlPatterns)
            assertEquals(Ordered.LOWEST_PRECEDENCE - 50, registration.order)
            val request = MockHttpServletRequest("GET", "/api/admin/sys/tenant/list")
            val response = MockHttpServletResponse()
            var handlerReached = false
            filter.doFilter(request, response) { _, _ -> handlerReached = true }
            assertEquals(503, response.status)
            assertFalse(handlerReached)
        }
    }
}
