package io.kudos.ability.distributed.discovery.nacos.loadbalancer

import org.springframework.cloud.client.ServiceInstance
import org.springframework.cloud.client.loadbalancer.Request
import org.springframework.cloud.client.loadbalancer.RequestDataContext
import org.springframework.cloud.client.loadbalancer.reactive.ReactiveLoadBalancer
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig
import org.springframework.cloud.loadbalancer.core.DelegatingServiceInstanceListSupplier
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier
import org.springframework.util.StringUtils
import reactor.core.publisher.Flux

/**
 * `ServiceInstanceListSupplier` that selects the zone of service instances based on a request header hint.
 *
 * Rules (see [filteredByHint]):
 *  - Request hint header has a value -> pick only instances whose `metadata.zoneMetadataKey` equals the hint; fall back to all when none match
 *  - Request hint header is empty + a default `LoadBalancerZoneConfig.zone` is configured ->
 *    pick instances whose `metadata.zoneMetadataKey` equals the default zone or is unset
 *  - Request hint header is empty + no default zone configured -> return all
 *
 * The hint header name is configured via the spring-cloud-loadbalancer standard property
 * `spring.cloud.loadbalancer.{serviceId}.hint-header-name` (default `X-SC-LB-Hint`).
 *
 * The **metadata field name** is injected through the [zoneMetadataKey] constructor parameter;
 * the default `"zone"` matches the spring-cloud-loadbalancer `ZONE` convention. If application code
 * tags nacos instances with a different field (e.g. `region` / `cluster-zone`), pass the
 * corresponding key during wiring.
 *
 * @author K
 * @since 1.0.0
 */
class HintZoneServiceInstanceListSupplier(
    delegate: ServiceInstanceListSupplier,
    private val zoneConfig: LoadBalancerZoneConfig,
    factory: ReactiveLoadBalancer.Factory<ServiceInstance>,
    private val zoneMetadataKey: String = DEFAULT_ZONE_METADATA_KEY,
) : DelegatingServiceInstanceListSupplier(delegate) {

    private val properties = checkNotNull(factory.getProperties(serviceId)) { "load balancer properties for $serviceId" }

    override fun get(): Flux<MutableList<ServiceInstance>> = delegate.get()

    override fun get(request: Request<*>): Flux<MutableList<ServiceInstance>> =
        delegate.get(request).map { instances ->
            filteredByHint(instances, getHint(request.getContext()), zoneConfig.zone, zoneMetadataKey)
        }

    /**
     * Extract the hint string from the load balancer request context.
     * Returns null when the context is null or not a [RequestDataContext] (e.g. a direct RPC call without HTTP headers).
     *
     * @param requestContext the Request context supplied by spring-cloud-loadbalancer
     * @return the hint string; null when not available
     * @author K
     * @since 1.0.0
     */
    private fun getHint(requestContext: Any?): String? =
        (requestContext as? RequestDataContext)?.let(::getHintFromHeader)

    /**
     * Read the hint from the HTTP client request header; the header name is configured via `spring.cloud.loadbalancer.{serviceId}.hint-header-name`.
     *
     * @param context the HTTP request context
     * @return the hint value from the header; null when absent
     * @author K
     * @since 1.0.0
     */
    private fun getHintFromHeader(context: RequestDataContext): String? =
        context.clientRequest?.headers?.getFirst(properties.hintHeaderName)

    companion object {
        /** Default field name matching spring-cloud-loadbalancer's internal `ZONE` constant. */
        const val DEFAULT_ZONE_METADATA_KEY: String = "zone"

        /**
         * Pure-function instance filter — extracted for unit testing, no dependency on Spring / Reactor context.
         *
         *  - `hint` non-empty -> pick only instances with `metadata[zoneMetadataKey] == hint`; return **all** when nothing matches
         *    (fallback so that misconfigured zones do not reject all business requests)
         *  - `hint` empty + `defaultZone` non-empty -> pick instances whose `metadata[zoneMetadataKey]` is empty or equals defaultZone
         *  - `hint` empty + `defaultZone` empty -> return unchanged (effectively no zone filtering)
         */
        internal fun filteredByHint(
            instances: MutableList<ServiceInstance>,
            hint: String?,
            defaultZone: String?,
            zoneMetadataKey: String,
        ): MutableList<ServiceInstance> {
            if (!StringUtils.hasText(hint)) {
                if (defaultZone.isNullOrBlank()) return instances
                // Pick only instances that were published without a zone or whose zone matches the default config
                return instances.filterTo(mutableListOf()) { instance ->
                    val zone = instance.metadata?.getOrDefault(zoneMetadataKey, "")
                    zone.isNullOrBlank() || zone == defaultZone
                }
            }

            val matched = instances.filterTo(mutableListOf()) {
                it.metadata?.getOrDefault(zoneMetadataKey, "") == hint
            }
            // Fall back to all instances when no hint match is found, so business requests are not fully rejected
            return if (matched.isNotEmpty()) matched else instances
        }
    }
}
