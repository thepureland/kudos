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
 * 按 Request Header Hint 选择服务实例 zone 的 `ServiceInstanceListSupplier`。
 *
 * 规则（[filteredByHint]）：
 *  - 请求 hint header 有值 → 只选 metadata.zone 与 hint 一致的实例；命中为空时降级返回全部
 *  - 请求 hint header 为空 + 配置了默认 `LoadBalancerZoneConfig.zone` →
 *    选 metadata.zone 与默认 zone 一致或未设 zone 的实例
 *  - 请求 hint header 为空 + 未配置默认 zone → 返回全部
 *
 * hint header 名通过 spring-cloud-loadbalancer 标准属性
 * `spring.cloud.loadbalancer.{serviceId}.hint-header-name` 配置（默认 `X-SC-LB-Hint`）。
 *
 * @author K
 * @since 1.0.0
 */
class HintZoneServiceInstanceListSupplier(
    delegate: ServiceInstanceListSupplier,
    private val zoneConfig: LoadBalancerZoneConfig,
    factory: ReactiveLoadBalancer.Factory<ServiceInstance>
) : DelegatingServiceInstanceListSupplier(delegate) {
    private val text = "zone"

    private val properties = checkNotNull(factory.getProperties(serviceId)) { "load balancer properties for $serviceId" }

    override fun get(): Flux<MutableList<ServiceInstance>> {
        return delegate.get()
    }

    override fun get(request: Request<*>): Flux<MutableList<ServiceInstance>> =
        delegate.get(request).map { instances ->
            filteredByHint(instances, getHint(request.getContext()))
        }

    private fun getHint(requestContext: Any?): String? {
        if (requestContext == null) {
            return null
        }
        var hint: String? = null
        if (requestContext is RequestDataContext) {
            hint = getHintFromHeader(requestContext)
        }
        return hint
    }

    private fun getHintFromHeader(context: RequestDataContext): String? {
        val headers = context.clientRequest?.headers
        return headers?.getFirst(properties.hintHeaderName)
    }

    private fun filteredByHint(instances: MutableList<ServiceInstance>, hint: String?): MutableList<ServiceInstance> {
        if (!StringUtils.hasText(hint)) {
            val defaultZone = zoneConfig.zone
            if (defaultZone.isNullOrBlank()) {
                return instances
            }

            val filteredInstances = mutableListOf<ServiceInstance>()
            for (serviceInstance in instances) {
                val serviceZone = serviceInstance.metadata?.getOrDefault(text, "")
                if (serviceZone.isNullOrBlank() || serviceZone == zoneConfig.zone) {
                    //只取服务实际发布时,未配置zone || 与默认配置一致
                    filteredInstances.add(serviceInstance)
                }
            }
            //only get default zone
            return filteredInstances
        }

        val filteredInstances = mutableListOf<ServiceInstance>()
        for (serviceInstance in instances) {
            if (serviceInstance.metadata?.getOrDefault(text, "") == hint) {
                filteredInstances.add(serviceInstance)
            }
        }
        if (filteredInstances.isNotEmpty()) {
            return filteredInstances
        }

        // If instances cannot be found based on hint,
        // we return all instances retrieved for given service id.
        return instances
    }
}
