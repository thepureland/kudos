package io.kudos.ability.security.enforcement.init

import io.kudos.ability.security.enforcement.filter.PermissionEnforcementFilter
import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ability.security.enforcement.port.IPermissionPointRegistry
import org.mockito.Mockito.mock
import org.springframework.beans.factory.support.DefaultListableBeanFactory
import org.springframework.boot.web.servlet.FilterRegistrationBean
import kotlin.test.Test
import kotlin.test.assertFailsWith

internal class EnforcementBackendSafetyTest {
    @Test
    fun enabledEnforcementRequiresBothPortsAndAnEnabledRegistration() {
        val beans = DefaultListableBeanFactory()
        val properties = EnforcementProperties().apply { enabled = true; shadowMode = false }
        val safety = EnforcementAutoConfiguration().enforcementBackendSafetyCheck(properties, beans)
        assertFailsWith<IllegalStateException> { safety.afterSingletonsInstantiated() }
        val decisions = mock(IAuthzDecisionProvider::class.java)
        val registry = mock(IPermissionPointRegistry::class.java)
        beans.registerSingleton("decisions", decisions)
        beans.registerSingleton("registry", registry)
        assertFailsWith<IllegalStateException> { safety.afterSingletonsInstantiated() }
        val registration = FilterRegistrationBean(PermissionEnforcementFilter(decisions, registry, properties))
        registration.isEnabled = false
        beans.registerSingleton("permissionEnforcementFilterRegistration", registration)
        assertFailsWith<IllegalStateException> { safety.afterSingletonsInstantiated() }
        registration.isEnabled = true
        safety.afterSingletonsInstantiated()
    }
}
