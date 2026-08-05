package io.kudos.ability.log.audit.common.starter

import io.kudos.ability.log.audit.common.api.LoggingMonitorService
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Unit tests for [LogAuditCommonConfiguration].
 *
 * The aspects' Spring wiring is exercised by the aspect integration tests; here we pin the
 * configuration surface itself:
 *  - each `@Bean` factory method returns a fresh non-null instance of the declared type,
 *  - the [IMonitorService] fallback is the [LoggingMonitorService],
 *  - the component name reported to the kudos initializer dispatcher is stable.
 *
 * @author K
 * @since 1.0.0
 */
internal class LogAuditCommonConfigurationTest {

    private val config = LogAuditCommonConfiguration()

    @Test
    fun beanFactoryMethods_returnFreshInstances() {
        assertNotNull(config.logAuditAspect())
        assertNotNull(config.webLogAuditAspect())
        assertNotSame(config.logAuditAspect(), config.logAuditAspect(),
            "without the CGLIB proxy each call yields a new instance — Spring handles singleton-caching")
    }

    @Test
    fun monitorServiceFallback_isLoggingImplementation() {
        assertIs<LoggingMonitorService>(config.loggingMonitorService(),
            "the @ConditionalOnMissingBean fallback must be the slf4j-backed implementation")
    }

    @Test
    fun componentName_isStable() {
        assertEquals("kudos-ability-log-audit-common", config.getComponentName())
    }
}
