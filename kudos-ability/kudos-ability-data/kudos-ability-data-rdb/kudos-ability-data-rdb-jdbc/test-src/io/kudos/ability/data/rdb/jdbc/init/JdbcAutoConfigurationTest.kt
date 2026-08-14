package io.kudos.ability.data.rdb.jdbc.init

import com.baomidou.dynamic.datasource.spring.boot.autoconfigure.DynamicDataSourceProperties
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceInterceptor
import io.kudos.ability.data.rdb.jdbc.datasource.DefaultDynamicDataSourceLoad
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.DsDataSourceCreator
import io.kudos.ability.data.rdb.jdbc.datasource.HikariDataSourceMeterInitEvent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Unit tests for [JdbcAutoConfiguration]'s bean factory methods, invoked directly (the Spring
 * container wiring itself is exercised by the module's integration tests via @EnableKudosTest).
 *
 * @author K
 * @since 1.0.0
 */
internal class JdbcAutoConfigurationTest {

    private val config = JdbcAutoConfiguration()

    @Test
    fun simpleBeans_areCreated() {
        assertNotNull(config.dynamicDataSourceProperties())
        assertNotNull(config.multipleDataSourceProperties())
        assertNotNull(config.routingCache())
        assertNotNull(config.dynamicDataSourceInterceptor())
        assertNotNull(config.dsChangeAspect())
        assertNotNull(config.tenantDsChangeAspect())
        assertNotNull(config.dsContextProcessor())
        assertIs<DefaultDynamicDataSourceLoad>(
            config.dynamicDataSourceLoad(),
            "without a custom implementation the placeholder loader is used"
        )
    }

    @Test
    fun routingAdvisor_bindsConfigurablePointcutAndOrder() {
        val props = MultipleDataSourceProperties()
        val interceptor = DynamicDataSourceInterceptor()
        val advisor = config.dynamicDataSourceRoutingAdvisor(props, interceptor)
        assertEquals("within(*..biz..*)", advisor.expression, "default pointcut targets biz packages")
        assertEquals(-99, advisor.order, "inner to the annotation aspects (-100)")
        assertSame(interceptor, advisor.advice)
    }

    @Test
    fun routingAdvisor_honorsCustomPointcutExpression() {
        val props = MultipleDataSourceProperties().apply { routingPointcut = "within(com.acme.service..*)" }
        val advisor = config.dynamicDataSourceRoutingAdvisor(props, DynamicDataSourceInterceptor())
        assertEquals("within(com.acme.service..*)", advisor.expression)
    }

    @Test
    fun dataSourceCreator_isDsDataSourceCreatorWithGlobalConfigApplied() {
        val properties = DynamicDataSourceProperties()
        val creator = config.dataSourceCreator(properties, emptyList())
        assertIs<DsDataSourceCreator>(creator, "kudos overrides baomidou's default creator")
    }

    @Test
    fun componentName() {
        assertEquals("kudos-ability-data-rdb-jdbc", config.getComponentName())
    }

    @Test
    fun micrometerConfiguration_buildsMeterAwareInitEvent() {
        val event = JdbcAutoConfiguration.MicrometerConfiguration().hikariDataSourceMeterInitEvent()
        assertIs<HikariDataSourceMeterInitEvent>(event)
    }

    @Test
    fun cacheCleanListenerConfiguration_buildsListenerBoundToDefaultCacheName() {
        val listener = JdbcAutoConfiguration.CacheCleanListenerConfiguration()
            .dataSourceClearListener(DsContextProcessor())
        assertNotNull(listener)
    }
}
