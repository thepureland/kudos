package io.kudos.ms.auth.core.authentication.assurance

import io.kudos.ms.auth.common.authentication.annotation.RequiresAuthenticationAssurance
import io.kudos.ms.auth.common.authentication.vo.AuthenticationSession
import org.springframework.aop.aspectj.annotation.AspectJProxyFactory
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class RequiresAuthenticationAssuranceAspectTest {

    private val now = Instant.parse("2026-08-25T10:00:00Z")
    private val aspect = RequiresAuthenticationAssuranceAspect(
        AuthenticationAssuranceVerifier(
            DefaultAuthenticationAssurancePolicy(),
            Clock.fixed(now, ZoneOffset.UTC),
        )
    )

    @AfterTest
    fun resetRequest() = RequestContextHolder.resetRequestAttributes()

    @Test
    fun annotatedMethod_withoutTrustedSession_failsClosed() {
        bindRequest(null)
        val target = proxy(MethodSecuredOperations())

        val error = assertFailsWith<AuthenticationAssuranceRequiredException> { target.execute() }

        assertEquals(AuthenticationAssuranceReasonEnum.AUTHENTICATION_REQUIRED, error.reason)
    }

    @Test
    fun annotatedMethod_withSatisfyingSession_proceeds() {
        bindRequest(session(DefaultAuthenticationAssurancePolicy.ACR_MFA))
        val target = proxy(MethodSecuredOperations())

        assertEquals("executed", target.execute())
    }

    @Test
    fun methodDeclaration_tightensClassDefault() {
        bindRequest(session(DefaultAuthenticationAssurancePolicy.ACR_PASSWORD))
        val target = proxy(ClassSecuredOperations())

        assertEquals("ordinary", target.ordinary())
        val error = assertFailsWith<AuthenticationAssuranceRequiredException> { target.sensitive() }
        assertEquals(AuthenticationAssuranceReasonEnum.INSUFFICIENT_ACR, error.reason)
    }

    private fun bindRequest(session: AuthenticationSession?) {
        val request = MockHttpServletRequest()
        session?.let { request.setAttribute(AuthenticationSession.REQUEST_ATTRIBUTE, it) }
        RequestContextHolder.setRequestAttributes(ServletRequestAttributes(request))
    }

    private fun <T : Any> proxy(target: T): T {
        val factory = AspectJProxyFactory(target)
        factory.addAspect(aspect)
        @Suppress("UNCHECKED_CAST")
        return factory.getProxy() as T
    }

    private fun session(acr: String) = AuthenticationSession(
        id = "s-1",
        tenantId = "t-1",
        userId = "u-1",
        authTime = now,
        amr = setOf("password"),
        acr = acr,
        createdAt = now.minusSeconds(60),
        lastSeenAt = now,
        idleExpiresAt = now.plusSeconds(600),
        absoluteExpiresAt = now.plusSeconds(3_600),
    )

    open class MethodSecuredOperations {
        @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_MFA)
        open fun execute() = "executed"
    }

    @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_PASSWORD)
    open class ClassSecuredOperations {
        open fun ordinary() = "ordinary"

        @RequiresAuthenticationAssurance(acr = DefaultAuthenticationAssurancePolicy.ACR_MFA)
        open fun sensitive() = "sensitive"
    }
}
