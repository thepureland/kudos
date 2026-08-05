package io.kudos.ability.log.audit.mq.init

import io.kudos.ability.log.audit.mq.beans.MqAuditService
import io.kudos.ability.log.audit.mq.beans.MqMonitorService
import org.springframework.context.annotation.Primary
import org.springframework.context.annotation.PropertySource
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame
import kotlin.test.assertTrue

/**
 * [LogAuditMqAutoConfiguration] unit tests.
 *
 * Covers: both bean factory methods return the MQ implementations (and fresh instances per call —
 * singleton scoping is the container's job, not the factory method's), both factory methods carry
 * `@Primary` so the MQ implementations win over RDB/SLF4J fallbacks, the `@PropertySource`
 * declaration points to a yml file that actually exists on the classpath, and the component name
 * matches the module name.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class LogAuditMqAutoConfigurationTest {

    private val config = LogAuditMqAutoConfiguration()

    @Test
    fun mqAuditService_returnsMqImplementation() {
        assertIs<MqAuditService>(config.mqAuditService())
    }

    @Test
    fun mqMonitorService_returnsMqImplementation() {
        assertIs<MqMonitorService>(config.mqMonitorService())
    }

    @Test
    fun beanFactoryMethods_returnFreshInstancesPerCall() {
        // singleton scoping is the Spring container's responsibility; the factory itself must not cache
        assertNotSame(config.mqAuditService(), config.mqAuditService())
        assertNotSame(config.mqMonitorService(), config.mqMonitorService())
    }

    @Test
    fun beanFactoryMethods_areMarkedPrimary() {
        val auditMethod = LogAuditMqAutoConfiguration::class.java.getMethod("mqAuditService")
        val monitorMethod = LogAuditMqAutoConfiguration::class.java.getMethod("mqMonitorService")

        assertNotNull(
            auditMethod.getAnnotation(Primary::class.java),
            "mqAuditService must be @Primary so the MQ implementation wins when an RDB implementation coexists",
        )
        assertNotNull(
            monitorMethod.getAnnotation(Primary::class.java),
            "mqMonitorService must be @Primary so the MQ implementation wins over the SLF4J fallback",
        )
    }

    @Test
    fun propertySource_pointsToExistingClasspathYml() {
        val propertySource = LogAuditMqAutoConfiguration::class.java.getAnnotation(PropertySource::class.java)

        assertNotNull(propertySource, "configuration must import kudos-ability-log-audit-mq.yml bindings")
        assertEquals(listOf("classpath:kudos-ability-log-audit-mq.yml"), propertySource.value.toList())

        val resource = propertySource.value.single().removePrefix("classpath:")
        assertNotNull(
            Thread.currentThread().contextClassLoader.getResource(resource),
            "declared property source '$resource' must exist on the classpath, otherwise context startup fails",
        )
    }

    @Test
    fun getComponentName_returnsModuleName() {
        assertEquals("kudos-ability-log-audit-mq", config.getComponentName())
    }

    @Test
    fun getComponentName_isStableAcrossCalls() {
        assertTrue((1..3).map { config.getComponentName() }.all { it == "kudos-ability-log-audit-mq" })
    }

}
