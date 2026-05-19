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
 *  - 请求 hint header 有值 → 只选 `metadata.zoneMetadataKey` 与 hint 一致的实例；命中为空时降级返回全部
 *  - 请求 hint header 为空 + 配置了默认 `LoadBalancerZoneConfig.zone` →
 *    选 `metadata.zoneMetadataKey` 与默认 zone 一致或未设 zone 的实例
 *  - 请求 hint header 为空 + 未配置默认 zone → 返回全部
 *
 * hint header 名通过 spring-cloud-loadbalancer 标准属性
 * `spring.cloud.loadbalancer.{serviceId}.hint-header-name` 配置（默认 `X-SC-LB-Hint`）。
 *
 * **metadata 字段名**通过构造参数 [zoneMetadataKey] 注入，默认 `"zone"` 与 spring-cloud-loadbalancer
 * 的 `ZONE` 约定一致。业务侧如果 nacos 实例上挂的是别的字段名（如 `region` / `cluster-zone`），
 * 装配处传入对应键。
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

    override fun get(): Flux<MutableList<ServiceInstance>> {
        return delegate.get()
    }

    override fun get(request: Request<*>): Flux<MutableList<ServiceInstance>> =
        delegate.get(request).map { instances ->
            filteredByHint(instances, getHint(request.getContext()), zoneConfig.zone, zoneMetadataKey)
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

    companion object {
        /** 与 spring-cloud-loadbalancer 内部 `ZONE` 常量一致的默认字段名。 */
        const val DEFAULT_ZONE_METADATA_KEY: String = "zone"

        /**
         * 纯函数版本的实例过滤——抽出来便于单测，不依赖 Spring / Reactor 任何上下文。
         *
         *  - `hint` 非空 → 只选 `metadata[zoneMetadataKey] == hint` 的实例；命中为空时返回**全部**
         *    （降级，避免业务请求因为 zone 配错被完全拒绝）
         *  - `hint` 为空 + `defaultZone` 非空 → 选 `metadata[zoneMetadataKey]` 为空或等于 defaultZone 的实例
         *  - `hint` 为空 + `defaultZone` 为空 → 原样返回（等效不做 zone 过滤）
         */
        internal fun filteredByHint(
            instances: MutableList<ServiceInstance>,
            hint: String?,
            defaultZone: String?,
            zoneMetadataKey: String,
        ): MutableList<ServiceInstance> {
            if (!StringUtils.hasText(hint)) {
                if (defaultZone.isNullOrBlank()) {
                    return instances
                }

                val filteredInstances = mutableListOf<ServiceInstance>()
                for (serviceInstance in instances) {
                    val serviceZone = serviceInstance.metadata?.getOrDefault(zoneMetadataKey, "")
                    if (serviceZone.isNullOrBlank() || serviceZone == defaultZone) {
                        // 只取服务实际发布时,未配置zone || 与默认配置一致
                        filteredInstances.add(serviceInstance)
                    }
                }
                return filteredInstances
            }

            val filteredInstances = mutableListOf<ServiceInstance>()
            for (serviceInstance in instances) {
                if (serviceInstance.metadata?.getOrDefault(zoneMetadataKey, "") == hint) {
                    filteredInstances.add(serviceInstance)
                }
            }
            if (filteredInstances.isNotEmpty()) {
                return filteredInstances
            }

            // If instances cannot be found based on hint, return all instances for given service id.
            return instances
        }
    }
}
