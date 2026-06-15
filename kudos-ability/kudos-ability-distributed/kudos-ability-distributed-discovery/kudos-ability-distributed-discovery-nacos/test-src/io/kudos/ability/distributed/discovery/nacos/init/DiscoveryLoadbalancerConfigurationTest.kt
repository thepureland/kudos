package io.kudos.ability.distributed.discovery.nacos.init

import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient
import org.springframework.cloud.client.loadbalancer.DefaultRequest
import org.springframework.cloud.client.loadbalancer.LoadBalancerClientsProperties
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties
import org.springframework.cloud.client.loadbalancer.RequestData
import org.springframework.cloud.client.loadbalancer.RequestDataContext
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory
import org.springframework.context.support.GenericApplicationContext
import org.springframework.core.Ordered
import org.springframework.core.env.MapPropertySource
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.util.LinkedMultiValueMap
import reactor.core.publisher.Flux
import java.net.URI
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull

/**
 * Unit tests for [DiscoveryLoadbalancerConfiguration] — the suppliers are assembled against a
 * plain [GenericApplicationContext] holding fake discovery clients (no Nacos / web server needed).
 *
 * Coverage:
 *  - blocking branch: supplier bean is built, wrapped with the default blocking order, exposes the
 *    delegate service id, and filters instances by the request hint header through the whole chain
 *  - blocking branch: get() without a request returns the unfiltered delegate list
 *  - reactive branch: supplier bean is built with the default reactive order and filters by hint
 *  - the supplier order can be overridden via
 *    `kudos.ability.distributed.discovery.nacos.loadbalancer.service-instance-supplier-order`
 *  - the zone metadata key can be overridden via
 *    `kudos.ability.distributed.discovery.nacos.zone-metadata-key` (blank value falls back to "zone")
 *  - the @PostConstruct log markers of both nested configurations run without error
 *
 * @author K
 * @since 1.0.0
 */
internal class DiscoveryLoadbalancerConfigurationTest {

    private val east = instance("svc-east", "zone", "east")
    private val west = instance("svc-west", "zone", "west")

    @Test
    fun blockingSupplier_hasDefaultOrderAndServiceId_andFiltersByHintHeader() {
        newBlockingContext(listOf(east, west)).use { ctx ->
            val supplier = DiscoveryLoadbalancerConfiguration.BlockingSupportConfiguration()
                .zonePreferenceDiscoveryClientServiceInstanceListSupplier(ctx)

            assertNotNull(supplier)
            val ordered = assertIs<Ordered>(supplier)
            assertEquals(
                DiscoveryLoadbalancerConfiguration.DEFAULT_BLOCKING_SERVICE_INSTANCE_SUPPLIER_ORDER,
                ordered.order
            )
            assertEquals("svc", supplier.serviceId)

            val filtered = assertNotNull(supplier.get(requestWithHint("east")).blockFirst(Duration.ofSeconds(10)))
            assertEquals(listOf<ServiceInstance>(east), filtered)
        }
    }

    @Test
    fun blockingSupplier_getWithoutRequest_returnsAllInstances() {
        newBlockingContext(listOf(east, west)).use { ctx ->
            val supplier = DiscoveryLoadbalancerConfiguration.BlockingSupportConfiguration()
                .zonePreferenceDiscoveryClientServiceInstanceListSupplier(ctx)

            assertNotNull(supplier)
            val all = assertNotNull(supplier.get().blockFirst(Duration.ofSeconds(10)))
            assertEquals(listOf<ServiceInstance>(east, west), all)
        }
    }

    @Test
    fun blockingSupplier_orderOverriddenByProperty() {
        val props = mapOf(DiscoveryLoadbalancerConfiguration.SERVICE_INSTANCE_SUPPLIER_ORDER_PROPERTY to "42")
        newBlockingContext(listOf(east), props).use { ctx ->
            val supplier = DiscoveryLoadbalancerConfiguration.BlockingSupportConfiguration()
                .zonePreferenceDiscoveryClientServiceInstanceListSupplier(ctx)

            assertEquals(42, assertIs<Ordered>(supplier).order)
        }
    }

    @Test
    fun blockingSupplier_customZoneMetadataKey_filtersByThatKey() {
        val regionEast = instance("svc-east", "region", "east")
        val regionWest = instance("svc-west", "region", "west")
        val props = mapOf("kudos.ability.distributed.discovery.nacos.zone-metadata-key" to "region")
        newBlockingContext(listOf(regionEast, regionWest), props).use { ctx ->
            val supplier = DiscoveryLoadbalancerConfiguration.BlockingSupportConfiguration()
                .zonePreferenceDiscoveryClientServiceInstanceListSupplier(ctx)

            assertNotNull(supplier)
            val filtered = assertNotNull(supplier.get(requestWithHint("west")).blockFirst(Duration.ofSeconds(10)))
            assertEquals(listOf<ServiceInstance>(regionWest), filtered)
        }
    }

    @Test
    fun blockingSupplier_blankZoneMetadataKey_fallsBackToDefaultZoneKey() {
        val props = mapOf("kudos.ability.distributed.discovery.nacos.zone-metadata-key" to "")
        newBlockingContext(listOf(east, west), props).use { ctx ->
            val supplier = DiscoveryLoadbalancerConfiguration.BlockingSupportConfiguration()
                .zonePreferenceDiscoveryClientServiceInstanceListSupplier(ctx)

            assertNotNull(supplier)
            val filtered = assertNotNull(supplier.get(requestWithHint("west")).blockFirst(Duration.ofSeconds(10)))
            assertEquals(listOf<ServiceInstance>(west), filtered, "blank key must fall back to the default 'zone'")
        }
    }

    @Test
    fun reactiveSupplier_hasDefaultOrder_andFiltersByHintHeader() {
        newReactiveContext(listOf(east, west)).use { ctx ->
            val supplier = DiscoveryLoadbalancerConfiguration.ReactiveSupportConfiguration()
                .zonePreferenceDiscoveryClientServiceInstanceListSupplier(ctx)

            assertNotNull(supplier)
            val ordered = assertIs<Ordered>(supplier)
            assertEquals(
                DiscoveryLoadbalancerConfiguration.DEFAULT_REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER,
                ordered.order
            )
            assertEquals("svc", supplier.serviceId)

            val filtered = assertNotNull(supplier.get(requestWithHint("east")).blockFirst(Duration.ofSeconds(10)))
            assertEquals(listOf<ServiceInstance>(east), filtered)
        }
    }

    @Test
    fun postConstructLogMarkers_runWithoutError() {
        // Outer configuration class has no logic beyond construction
        DiscoveryLoadbalancerConfiguration()
        DiscoveryLoadbalancerConfiguration.BlockingSupportConfiguration().init()
        DiscoveryLoadbalancerConfiguration.ReactiveSupportConfiguration().init()
    }

    // ----- helpers -----

    private fun newBlockingContext(
        instances: List<ServiceInstance>,
        extraProperties: Map<String, Any> = emptyMap(),
    ): GenericApplicationContext = newContext(extraProperties) { ctx ->
        ctx.beanFactory.registerSingleton("discoveryClient", FakeDiscoveryClient(instances))
    }

    private fun newReactiveContext(
        instances: List<ServiceInstance>,
        extraProperties: Map<String, Any> = emptyMap(),
    ): GenericApplicationContext = newContext(extraProperties) { ctx ->
        ctx.beanFactory.registerSingleton("reactiveDiscoveryClient", FakeReactiveDiscoveryClient(instances))
    }

    private fun newContext(
        extraProperties: Map<String, Any>,
        registerDiscoveryClient: (GenericApplicationContext) -> Unit,
    ): GenericApplicationContext {
        val ctx = GenericApplicationContext()
        val properties = mutableMapOf<String, Any>("loadbalancer.client.name" to "svc")
        properties.putAll(extraProperties)
        ctx.environment.propertySources.addFirst(MapPropertySource("test", properties))
        registerDiscoveryClient(ctx)
        ctx.beanFactory.registerSingleton("loadBalancerClientFactory", FixedPropertiesLoadBalancerClientFactory())
        ctx.beanFactory.registerSingleton("loadBalancerZoneConfig", LoadBalancerZoneConfig(null))
        ctx.refresh()
        return ctx
    }

    private fun requestWithHint(hint: String): DefaultRequest<RequestDataContext> {
        val headers = HttpHeaders()
        headers.set("X-SC-LB-Hint", hint)
        val requestData = RequestData(
            HttpMethod.GET, URI.create("http://svc/api"), headers, LinkedMultiValueMap(), mutableMapOf()
        )
        return DefaultRequest(RequestDataContext(requestData))
    }

    private fun instance(id: String, metadataKey: String, zone: String): ServiceInstance =
        DefaultServiceInstance(id, "svc", "host-$id", 80, false, mapOf(metadataKey to zone))

    /**
     * Fake blocking [DiscoveryClient] serving a fixed instance list.
     *
     * @author K
     * @since 1.0.0
     */
    private class FakeDiscoveryClient(
        private val instances: List<ServiceInstance>,
    ) : DiscoveryClient {
        override fun description(): String = "fake blocking discovery client"
        override fun getInstances(serviceId: String): MutableList<ServiceInstance> = instances.toMutableList()
        override fun getServices(): MutableList<String> = mutableListOf("svc")
    }

    /**
     * Fake [ReactiveDiscoveryClient] serving a fixed instance list.
     *
     * @author K
     * @since 1.0.0
     */
    private class FakeReactiveDiscoveryClient(
        private val instances: List<ServiceInstance>,
    ) : ReactiveDiscoveryClient {
        override fun description(): String = "fake reactive discovery client"
        override fun getInstances(serviceId: String): Flux<ServiceInstance> = Flux.fromIterable(instances)
        override fun getServices(): Flux<String> = Flux.just("svc")
    }

    /**
     * [LoadBalancerClientFactory] stub that serves default [LoadBalancerProperties] without
     * spinning up named child contexts.
     *
     * @author K
     * @since 1.0.0
     */
    private class FixedPropertiesLoadBalancerClientFactory :
        LoadBalancerClientFactory(LoadBalancerClientsProperties()) {
        override fun getProperties(serviceId: String?): LoadBalancerProperties = LoadBalancerProperties()
    }
}
