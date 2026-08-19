package io.kudos.ability.cache.interservice.client.init

import io.kudos.ability.cache.common.core.keyvalue.IKeyValueCacheManager
import io.kudos.ability.cache.interservice.client.core.ClientCacheHelper
import io.kudos.ability.cache.interservice.client.http.HttpCacheNegotiationInterceptor
import org.springframework.beans.factory.ObjectProvider
import org.springframework.core.Ordered
import org.springframework.web.client.RestClient
import org.springframework.web.service.registry.HttpServiceGroup
import org.springframework.web.service.registry.HttpServiceGroupConfigurer
import java.util.function.Predicate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for the bean factory methods of [InterServiceCacheClientAutoConfiguration].
 *
 * Calls each `@Bean` method directly (no Spring container) and asserts:
 *  - default [InterServiceCacheClientProperties] are produced;
 *  - [ClientCacheHelper] respects presence/absence of the local cache manager (`ObjectProvider.ifAvailable`);
 *  - the group configurer installs one [HttpCacheNegotiationInterceptor] per group;
 *  - the component name is reported.
 *
 * The Feign-era test additionally asserted that the `@Primary` `feignDecoder` bean wrapped Spring
 * Cloud's Optional/ResponseEntity/Jackson decoder chain and that the chain decoded a body. There is
 * no such bean any more, and deliberately so: overriding a framework bean application-wide to add one
 * feature was the riskiest part of that design. `RestClient` runs the application's own message
 * converters, so nothing is replaced and there is nothing left to assert there.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class InterServiceCacheClientAutoConfigurationTest {

    private val config = InterServiceCacheClientAutoConfiguration()

    @Test
    fun properties_defaults() {
        val properties = config.interServiceCacheClientProperties()
        assertEquals(600, properties.ttlSeconds)
        assertTrue(properties.includeGroups.isEmpty())
        assertTrue(properties.excludeGroups.isEmpty())
    }

    @Test
    fun clientCacheHelper_withoutCacheManager_hasNoLocalCache() {
        val helper = config.clientCacheHelper(InterServiceCacheClientProperties(), EmptyProvider())
        assertFalse(helper.hasLocalCache())
    }

    @Test
    fun groupConfigurer_installsOneInterceptorPerGroup() {
        val helper = config.clientCacheHelper(InterServiceCacheClientProperties(), EmptyProvider())
        val configurer = config.interServiceCacheGroupConfigurer(
            helper, InterServiceCacheClientProperties(), "test-app"
        )
        assertEquals(Ordered.LOWEST_PRECEDENCE, configurer.order)

        val builders = mapOf(
            "groupA" to RestClient.builder(),
            "groupB" to RestClient.builder(),
        )
        configurer.configureGroups(RecordingGroups(builders))

        // Each group's builder must have received exactly one negotiation interceptor.
        builders.forEach { (name, builder) ->
            val installed = mutableListOf<Any>()
            builder.requestInterceptors { interceptors -> installed.addAll(interceptors) }
            assertEquals(
                1,
                installed.count { it is HttpCacheNegotiationInterceptor },
                "group $name should carry exactly one negotiation interceptor"
            )
        }
    }

    @Test
    fun componentName() {
        assertNotNull(config.getComponentName())
        assertEquals("kudos-ability-cache-interservice-client", config.getComponentName())
    }

    /** `ObjectProvider` that never yields a cache manager. */
    private class EmptyProvider : ObjectProvider<IKeyValueCacheManager<*>> {
        override fun getObject(vararg args: Any?): IKeyValueCacheManager<*> = throw UnsupportedOperationException()
        override fun getObject(): IKeyValueCacheManager<*> = throw UnsupportedOperationException()
        override fun getIfAvailable(): IKeyValueCacheManager<*>? = null
        override fun getIfUnique(): IKeyValueCacheManager<*>? = null
    }

    /** Minimal [HttpServiceGroupConfigurer.Groups] that replays a fixed set of named builders. */
    private class RecordingGroups(
        private val builders: Map<String, RestClient.Builder>
    ) : HttpServiceGroupConfigurer.Groups<RestClient.Builder> {

        override fun filterByName(vararg groupNames: String) = this

        override fun filter(predicate: Predicate<HttpServiceGroup>) = this

        override fun forEachClient(callback: HttpServiceGroupConfigurer.ClientCallback<RestClient.Builder>) {
            builders.forEach { (name, builder) -> callback.withClient(NamedGroup(name), builder) }
        }

        override fun forEachClient(callback: HttpServiceGroupConfigurer.InitializingClientCallback<RestClient.Builder>) {
            throw UnsupportedOperationException("not used by the configurer under test")
        }

        override fun forEachProxyFactory(callback: HttpServiceGroupConfigurer.ProxyFactoryCallback) {
            throw UnsupportedOperationException("not used by the configurer under test")
        }

        override fun forEachGroup(callback: HttpServiceGroupConfigurer.GroupCallback<RestClient.Builder>) {
            throw UnsupportedOperationException("not used by the configurer under test")
        }
    }

    private class NamedGroup(private val name: String) : HttpServiceGroup {
        override fun name(): String = name
        override fun httpServiceTypes(): Set<Class<*>> = emptySet()
        override fun clientType(): HttpServiceGroup.ClientType = HttpServiceGroup.ClientType.REST_CLIENT
    }
}
