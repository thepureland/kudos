package io.kudos.ability.data.rdb.jdbc.init

import io.kudos.ability.data.rdb.jdbc.aop.DsChangeAspect
import io.kudos.ability.data.rdb.jdbc.aop.DynamicDataSourceInterceptor
import io.kudos.ability.data.rdb.jdbc.aop.TenantDsChangeAspect
import io.kudos.ability.data.rdb.jdbc.datasource.DsContextProcessor
import io.kudos.ability.data.rdb.jdbc.datasource.RoutingCache
import io.kudos.test.common.init.EnableKudosTest
import jakarta.annotation.Resource
import org.springframework.aop.aspectj.AspectJExpressionPointcutAdvisor
import org.springframework.context.ApplicationContext
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Wiring-completeness test for [JdbcAutoConfiguration] in a real container.
 *
 * The module declares every bean explicitly (no component scanning), and the routing advice is
 * applied through an [AspectJExpressionPointcutAdvisor] rather than an `@Aspect` class. Advisor
 * beans are retrieved **eagerly** by Spring's auto-proxy infrastructure, well before ordinary
 * singletons are created — so this test pins down the thing that eagerness could plausibly break:
 * that the advisor reads a *bound* [MultipleDataSourceProperties] (yml values present) rather than
 * a freshly-constructed one still holding defaults.
 *
 * @author K
 * @since 1.0.0
 */
@EnableKudosTest
internal class JdbcWiringIntegrationTest {

    @Resource
    private lateinit var applicationContext: ApplicationContext

    @Resource
    private lateinit var properties: MultipleDataSourceProperties

    @Test
    fun configurationPropertiesAreBoundBeforeTheAdvisorReadsThem() {
        // yml-provided values must be visible on the properties bean...
        assertEquals(
            "ds1", properties.packageDataSource["com.example.wiringprobe.biz"],
            "packageDataSource must be bound from application.yml"
        )
        assertEquals("within(com.example.wiringprobe..*)", properties.routingPointcut)

        // ...and the eagerly-retrieved advisor must have captured the bound value, not the default
        val advisor = applicationContext.getBean(
            "dynamicDataSourceRoutingAdvisor", AspectJExpressionPointcutAdvisor::class.java
        )
        assertEquals(
            "within(com.example.wiringprobe..*)", advisor.expression,
            "the advisor read its pointcut after binding; a default here would mean the routing " +
                    "advice silently applies to the wrong packages"
        )
        assertEquals(-99, advisor.order, "inner to the annotation aspects (-100)")
    }

    @Test
    fun everyDeclaredBeanIsPresentExactlyOnce() {
        // the module dropped @Component in favour of explicit @Bean methods; a stray duplicate
        // registration (the old DsDataSourceCreator situation) would show up here
        assertEquals(1, applicationContext.getBeanNamesForType(DsContextProcessor::class.java).size)
        assertEquals(1, applicationContext.getBeanNamesForType(RoutingCache::class.java).size)
        assertEquals(1, applicationContext.getBeanNamesForType(DynamicDataSourceInterceptor::class.java).size)
        assertEquals(1, applicationContext.getBeanNamesForType(DsChangeAspect::class.java).size)
        assertEquals(1, applicationContext.getBeanNamesForType(TenantDsChangeAspect::class.java).size)
    }

    @Test
    fun routingCacheIsSharedBetweenTheInterceptorAndTheProcessor() {
        // the whole point of extracting RoutingCache: refreshDatasource() must invalidate the very
        // cache the interceptor reads, without either side reaching for the other's static state
        val cache = applicationContext.getBean(RoutingCache::class.java)
        val interceptorCache = readField(
            applicationContext.getBean(DynamicDataSourceInterceptor::class.java), "routingCache"
        )
        val processorCache = readField(applicationContext.getBean(DsContextProcessor::class.java), "routingCache")
        assertNotNull(interceptorCache)
        assertSame(cache, interceptorCache)
        assertSame(cache, processorCache)
    }

    private fun readField(target: Any, name: String): Any? {
        var cls: Class<*>? = target.javaClass
        while (cls != null) {
            try {
                return cls.getDeclaredField(name).also { it.isAccessible = true }.get(target)
            } catch (_: NoSuchFieldException) {
                cls = cls.superclass
            }
        }
        throw NoSuchFieldException(name)
    }
}
