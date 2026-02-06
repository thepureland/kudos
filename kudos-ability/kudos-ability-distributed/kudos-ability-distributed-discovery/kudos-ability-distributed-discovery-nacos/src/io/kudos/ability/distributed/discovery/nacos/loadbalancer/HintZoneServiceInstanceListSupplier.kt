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
import java.util.function.Function

/**
 * 服务实例列表提示者,通过Request Header Hint 选择分区
 */
class HintZoneServiceInstanceListSupplier(
    delegate: ServiceInstanceListSupplier,
    private val zoneConfig: LoadBalancerZoneConfig,
    factory: ReactiveLoadBalancer.Factory<ServiceInstance>
) : DelegatingServiceInstanceListSupplier(delegate) {
    private val text = "zone"

    private val properties = factory.getProperties(serviceId)!!

    override fun get(): Flux<MutableList<ServiceInstance>> {
        return delegate.get()
    }

    override fun get(request: Request<*>): Flux<MutableList<ServiceInstance>> {
        return delegate.get(request)
            .map(Function { instances: MutableList<ServiceInstance> ->
                filteredByHint(instances, getHint(request.getContext()))
            } as Function<in MutableList<ServiceInstance>, out MutableList<ServiceInstance>>)
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

        val filteredInstances: MutableList<ServiceInstance> = ArrayList(instances.size)
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
