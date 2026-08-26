package io.kudos.ms.auth.core.platform.init

import io.kudos.context.init.IComponentInitializer
import io.kudos.ms.auth.core.authentication.securityevent.policy.AuthSecurityEventSlaProperties
import io.kudos.ms.auth.core.authentication.securityevent.policy.AuthSecurityEventEscalationProperties
import io.kudos.ms.auth.core.authentication.securityevent.notification.routing.service.iservice.IAuthSecurityEventNotificationRouteConfigService
import io.kudos.ms.auth.core.authentication.securityevent.oncall.service.iservice.IAuthSecurityEventOnCallRosterService
import org.mockito.Mockito.mock
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pure unit test for [AuthAutoConfiguration]: the auto-configuration is an [IComponentInitializer]
 * whose component name identifies this atomic service. No Spring context is started — only the
 * component-name contract is asserted, since the configuration's wiring is exercised by every
 * container-backed integration test in this module.
 *
 * @author K
 * @since 1.0.0
 */
internal class AuthAutoConfigurationTest {

    @Test
    fun getComponentName_returnsModuleName() {
        val cfg = AuthAutoConfiguration()
        val initializer: IComponentInitializer = cfg
        assertEquals("kudos-ms-auth-core", initializer.getComponentName())
    }

    @Test
    fun defaultAssurancePolicy_isConservativeAndReplaceableBeanCandidate() {
        val configuration = AuthAutoConfiguration()
        val policy = configuration.authenticationAssurancePolicy()

        assertTrue(policy.isSatisfied("urn:kudos:acr:mfa", "urn:kudos:acr:password"))
        assertTrue(!policy.isSatisfied("urn:kudos:acr:password", "urn:kudos:acr:mfa"))
        val verifier = configuration.authenticationAssuranceVerifier(policy)
        assertNotNull(configuration.requiresAuthenticationAssuranceAspect(verifier))
        assertNotNull(configuration.authenticationAssuranceExceptionHandler())
        assertNotNull(configuration.totpEnrollmentStore())
        assertNotNull(configuration.authSecurityEventSlaPolicy(AuthSecurityEventSlaProperties()))
        assertNotNull(configuration.authSecurityEventEscalationPolicy(AuthSecurityEventEscalationProperties()))
        val responderResolver = configuration.authSecurityEventResponderResolver(
            mock(IAuthSecurityEventOnCallRosterService::class.java)
        )
        assertNotNull(responderResolver)
        assertNotNull(
            configuration.authSecurityEventNotificationRoutePolicy(
                mock(IAuthSecurityEventNotificationRouteConfigService::class.java),
                responderResolver,
            )
        )
    }
}
