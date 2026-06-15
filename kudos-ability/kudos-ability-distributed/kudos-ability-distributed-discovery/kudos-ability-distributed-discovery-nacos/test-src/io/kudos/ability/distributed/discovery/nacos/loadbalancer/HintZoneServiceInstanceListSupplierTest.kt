package io.kudos.ability.distributed.discovery.nacos.loadbalancer

import org.springframework.cloud.client.DefaultServiceInstance
import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.loadbalancer.DefaultRequest
import org.springframework.cloud.client.loadbalancer.LoadBalancerProperties
import org.springframework.cloud.client.loadbalancer.RequestData
import org.springframework.cloud.client.loadbalancer.RequestDataContext
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpMethod
import org.springframework.util.LinkedMultiValueMap
import reactor.core.publisher.Flux
import java.net.URI
import java.time.Duration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Unit tests for [HintZoneServiceInstanceListSupplier].
 *
 * Pure-function part ([HintZoneServiceInstanceListSupplier.filteredByHint]) — main branches:
 *  - **hint non-empty, has match** -> returns only matching instances
 *  - **hint non-empty, no match** -> falls back to the full instance list (so business calls are not rejected outright)
 *  - **hint empty + default zone non-empty** -> returns only instances whose zone matches the default, or have no zone set
 *  - **hint empty + default zone empty** -> returns the list as-is
 *
 * Also verifies the configurable [HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY]
 * scenario: switching the metadata field name to `region` still hits, and instances whose
 * metadata map is null are treated as untagged (never matching a concrete hint).
 *
 * Instance-level part (constructed with fake delegate / factory, no Spring context):
 *  - constructor fails fast when the factory returns no load balancer properties
 *  - get() without a request delegates unfiltered
 *  - get(request) filters by the hint header from the [RequestDataContext]
 *  - request context not a RequestDataContext / no client request / no hint header
 *    all degrade to the "no hint" path (default-zone filtering)
 */
internal class HintZoneServiceInstanceListSupplierTest {

    private val east = instance("svc-east", "east")
    private val west = instance("svc-west", "west")
    private val none = instance("svc-none", null)

    @Test
    fun hintHit_returnsOnlyMatching() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "east", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(listOf(east), result)
    }

    @Test
    fun hintMiss_returnsAllAsFallback() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "moon", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        // No matches -> fall back to the original list (by reference, confirming the fast path)
        assertSame(all, result)
    }

    @Test
    fun noHint_defaultZoneSet_returnsMatchingPlusUntagged() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = "east",
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(listOf(east, none), result, "Should include instances matching the default zone plus those with no zone set")
    }

    @Test
    fun noHint_blankDefaultZone_returnsAllAsIs() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = "",
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result, "Should return as-is when there is no default zone")
    }

    @Test
    fun noHint_nullDefaultZone_returnsAllAsIs() {
        val all = mutableListOf(east, west, none)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result)
    }

    @Test
    fun blankHint_treatedAsAbsent() {
        val all = mutableListOf(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "   ", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertSame(all, result, "Blank string should be treated the same as null")
    }

    @Test
    fun nullMetadata_noHintWithDefaultZone_treatedAsUntagged() {
        val nullMeta = NullMetadataInstance("svc-null-meta")
        val all = mutableListOf(east, west, nullMeta)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = null, defaultZone = "east",
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(
            listOf(east, nullMeta), result,
            "an instance whose metadata map is null must be treated like an untagged instance"
        )
    }

    @Test
    fun nullMetadata_hintGiven_neverMatchesHint() {
        val nullMeta = NullMetadataInstance("svc-null-meta")
        val all = mutableListOf(east, nullMeta)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "east", defaultZone = null,
            zoneMetadataKey = HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY,
        )
        assertEquals(listOf(east), result, "a null-metadata instance can never match a concrete hint")
    }

    @Test
    fun customMetadataKey_works() {
        val east = DefaultServiceInstance("svc-east", "svc", "h1", 80, false, mapOf("region" to "east"))
        val west = DefaultServiceInstance("svc-west", "svc", "h2", 80, false, mapOf("region" to "west"))
        val all = mutableListOf<ServiceInstance>(east, west)
        val result = HintZoneServiceInstanceListSupplier.filteredByHint(
            all, hint = "east", defaultZone = null,
            zoneMetadataKey = "region",
        )
        assertEquals(1, result.size)
        assertTrue(result.contains(east))
    }

    // ----- instance-level tests (fake delegate + factory, no Spring context) -----

    @Test
    fun constructor_whenFactoryReturnsNoProperties_failsFast() {
        val e = assertFailsWith<IllegalStateException> {
            HintZoneServiceInstanceListSupplier(
                FakeSupplier("svc", listOf(east, west)),
                LoadBalancerZoneConfig(null),
                FakeFactory(properties = null),
            )
        }
        assertTrue(e.message!!.contains("load balancer properties for svc"))
    }

    @Test
    fun get_withoutRequest_returnsDelegateListUnfiltered() {
        val supplier = supplier(listOf(east, west, none))
        val result = assertNotNull(supplier.get().blockFirst(Duration.ofSeconds(5)))
        assertEquals(listOf(east, west, none), result)
    }

    @Test
    fun getWithRequest_hintHeaderPresent_filtersByZone() {
        val supplier = supplier(listOf(east, west, none))
        val result = assertNotNull(supplier.get(requestWithHint("east")).blockFirst(Duration.ofSeconds(5)))
        assertEquals(listOf(east), result)
    }

    @Test
    fun getWithRequest_noHintHeader_defaultZoneApplied() {
        val supplier = supplier(listOf(east, west, none), defaultZone = "west")
        val result = assertNotNull(supplier.get(requestWithHint(null)).blockFirst(Duration.ofSeconds(5)))
        assertEquals(listOf(west, none), result, "default zone matches plus untagged instances")
    }

    @Test
    fun getWithRequest_contextNotRequestDataContext_returnsAll() {
        val supplier = supplier(listOf(east, west))
        val request = DefaultRequest<Any>("not-a-request-data-context")
        val result = assertNotNull(supplier.get(request).blockFirst(Duration.ofSeconds(5)))
        assertEquals(listOf(east, west), result)
    }

    @Test
    fun getWithRequest_contextWithoutClientRequest_returnsAll() {
        val supplier = supplier(listOf(east, west))
        val request = DefaultRequest(RequestDataContext())
        val result = assertNotNull(supplier.get(request).blockFirst(Duration.ofSeconds(5)))
        assertEquals(listOf(east, west), result)
    }

    @Test
    fun getWithRequest_customMetadataKey_filtersByThatKey() {
        val regionEast = DefaultServiceInstance("svc-east", "svc", "h1", 80, false, mapOf("region" to "east"))
        val regionWest = DefaultServiceInstance("svc-west", "svc", "h2", 80, false, mapOf("region" to "west"))
        val supplier = HintZoneServiceInstanceListSupplier(
            FakeSupplier("svc", listOf(regionEast, regionWest)),
            LoadBalancerZoneConfig(null),
            FakeFactory(LoadBalancerProperties()),
            zoneMetadataKey = "region",
        )
        val result = supplier.get(requestWithHint("west")).blockFirst(Duration.ofSeconds(5))
        assertNotNull(result)
        assertEquals(listOf<ServiceInstance>(regionWest), result)
    }

    private fun supplier(
        instances: List<ServiceInstance>,
        defaultZone: String? = null,
    ) = HintZoneServiceInstanceListSupplier(
        FakeSupplier("svc", instances),
        LoadBalancerZoneConfig(defaultZone),
        FakeFactory(LoadBalancerProperties()),
    )

    private fun requestWithHint(hint: String?): DefaultRequest<RequestDataContext> {
        val headers = HttpHeaders()
        if (hint != null) {
            // Default spring-cloud-loadbalancer hint header name
            headers.set("X-SC-LB-Hint", hint)
        }
        val requestData = RequestData(
            HttpMethod.GET, URI.create("http://svc/api"), headers, LinkedMultiValueMap(), mutableMapOf()
        )
        return DefaultRequest(RequestDataContext(requestData))
    }

    private fun instance(id: String, zone: String?): ServiceInstance {
        val metadata = if (zone == null) emptyMap() else mapOf("zone" to zone)
        return DefaultServiceInstance(id, "svc", "host-$id", 80, false, metadata)
    }

    /**
     * [ServiceInstance] whose metadata map is null — some discovery client implementations may
     * return null metadata; the filter must treat it as "no zone tag".
     *
     * @author K
     * @since 1.0.0
     */
    private class NullMetadataInstance(private val id: String) : ServiceInstance {
        override fun getInstanceId(): String = id
        override fun getServiceId(): String = "svc"
        override fun getHost(): String = "host-$id"
        override fun getPort(): Int = 80
        override fun isSecure(): Boolean = false
        override fun getUri(): URI = URI.create("http://host-$id:80")
        override fun getMetadata(): MutableMap<String, String>? = null
    }

    /**
     * Minimal in-memory [ServiceInstanceListSupplier] used as the delegate.
     *
     * @author K
     * @since 1.0.0
     */
    private class FakeSupplier(
        private val id: String,
        private val instances: List<ServiceInstance>,
    ) : ServiceInstanceListSupplier {
        override fun getServiceId(): String = id
        override fun get(): Flux<MutableList<ServiceInstance>> = Flux.just(instances.toMutableList())
    }

    /**
     * Fake [ReactiveLoadBalancer.Factory] that only serves load balancer properties.
     *
     * @author K
     * @since 1.0.0
     */
    private class FakeFactory(
        private val properties: LoadBalancerProperties?,
    ) : ReactiveLoadBalancer.Factory<ServiceInstance> {
        override fun getProperties(serviceId: String?): LoadBalancerProperties? = properties

        override fun getInstance(serviceId: String?): ReactiveLoadBalancer<ServiceInstance> =
            throw UnsupportedOperationException("not needed in tests")

        override fun <X : Any?> getInstances(name: String?, type: Class<X>?): MutableMap<String, X> =
            throw UnsupportedOperationException("not needed in tests")

        override fun <X : Any?> getInstance(name: String?, clazz: Class<*>?, vararg generics: Class<*>?): X =
            throw UnsupportedOperationException("not needed in tests")
    }
}
