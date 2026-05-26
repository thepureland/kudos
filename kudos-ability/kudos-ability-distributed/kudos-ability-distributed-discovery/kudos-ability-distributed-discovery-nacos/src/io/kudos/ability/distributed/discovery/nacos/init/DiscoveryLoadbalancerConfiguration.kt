package io.kudos.ability.distributed.discovery.nacos.init

import io.kudos.ability.distributed.discovery.nacos.loadbalancer.HintZoneServiceInstanceListSupplier
import io.kudos.base.logger.LogFactory
import jakarta.annotation.PostConstruct
import org.springframework.beans.factory.getBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.cloud.client.ConditionalOnBlockingDiscoveryEnabled
import org.springframework.cloud.client.ConditionalOnReactiveDiscoveryEnabled
import org.springframework.cloud.client.discovery.DiscoveryClient
import org.springframework.cloud.client.discovery.ReactiveDiscoveryClient
import org.springframework.cloud.client.loadbalancer.Request
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplierBuilder
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.Ordered
import reactor.core.publisher.Flux

/**
 * Custom load-balancing strategy: when `spring.cloud.loadbalancer.configurations=zone-preference`
 * is set, enable [HintZoneServiceInstanceListSupplier] — pick the target instance zone based on
 * the client request's `hint header`.
 *
 * Supports both blocking and reactive discovery clients; assembled on demand via conditional
 * annotations.
 *
 * @see com.alibaba.cloud.nacos.loadbalancer.NacosLoadBalancerClientConfiguration
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = [DiscoveryLoadbalancerConfiguration::class])
open class DiscoveryLoadbalancerConfiguration {


    companion object {
        private const val ZONE_METADATA_KEY_PROPERTY = "kudos.ability.distributed.discovery.nacos.zone-metadata-key"
        const val SERVICE_INSTANCE_SUPPLIER_ORDER_PROPERTY =
            "kudos.ability.distributed.discovery.nacos.loadbalancer.service-instance-supplier-order"
        const val DEFAULT_BLOCKING_SERVICE_INSTANCE_SUPPLIER_ORDER = 183827463
        const val DEFAULT_REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 183827465

        private fun hintZone(): ServiceInstanceListSupplierBuilder.DelegateCreator {
            return ServiceInstanceListSupplierBuilder.DelegateCreator { context: ConfigurableApplicationContext, delegate: ServiceInstanceListSupplier ->
                val loadBalancerClientFactory: LoadBalancerClientFactory = context.getBean<LoadBalancerClientFactory>()
                val zoneConfig: LoadBalancerZoneConfig = context.getBean(LoadBalancerZoneConfig::class.java)
                // Configurable metadata field name — nacos instances may carry region / cluster-zone etc.;
                // falls back to spring-cloud-loadbalancer's standard "zone" by default
                val zoneMetadataKey = context.environment.getProperty(ZONE_METADATA_KEY_PROPERTY)
                    ?.takeIf { it.isNotBlank() }
                    ?: HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY
                HintZoneServiceInstanceListSupplier(delegate, zoneConfig, loadBalancerClientFactory, zoneMetadataKey)
            }
        }

        private fun configuredOrder(context: ConfigurableApplicationContext, defaultOrder: Int): Int =
            context.environment.getProperty(SERVICE_INSTANCE_SUPPLIER_ORDER_PROPERTY, Int::class.java, defaultOrder)

        private fun ordered(
            delegate: ServiceInstanceListSupplier,
            context: ConfigurableApplicationContext,
            defaultOrder: Int
        ): ServiceInstanceListSupplier =
            OrderedServiceInstanceListSupplier(delegate, configuredOrder(context, defaultOrder))
    }

    /**
     * Hint-zone load-balancing support configuration for the blocking discovery client.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBlockingDiscoveryEnabled
    open class BlockingSupportConfiguration {
        @Bean
        @ConditionalOnBean(DiscoveryClient::class) //@ConditionalOnMissingBean
        @ConditionalOnProperty(value = ["spring.cloud.loadbalancer.configurations"], havingValue = "zone-preference")
        open fun zonePreferenceDiscoveryClientServiceInstanceListSupplier(
            context: ConfigurableApplicationContext
        ): ServiceInstanceListSupplier? {
            val supplier = ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient()
                .with(hintZone()) // Apply: service-instance header hint strategy
                .build(context)
            return ordered(supplier, context, DEFAULT_BLOCKING_SERVICE_INSTANCE_SUPPLIER_ORDER)
        }

        /**
         * Used purely to mark in the logs that the blocking branch has been assembled — lets ops
         * see at a glance which LB path is in use.
         *
         * @author K
         * @since 1.0.0
         */
        @PostConstruct
        fun init() {
            LogFactory.getLog(this::class).info("[blocking hint zone preference] initialization complete...")
        }
    }

    /**
     * Hint-zone load-balancing support configuration for the reactive discovery client.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnReactiveDiscoveryEnabled
    open class ReactiveSupportConfiguration {
        @Bean
        @ConditionalOnBean(ReactiveDiscoveryClient::class) //@ConditionalOnMissingBean
        @ConditionalOnProperty(value = ["spring.cloud.loadbalancer.configurations"], havingValue = "zone-preference")
        open fun zonePreferenceDiscoveryClientServiceInstanceListSupplier(
            context: ConfigurableApplicationContext
        ): ServiceInstanceListSupplier? {
            val supplier = ServiceInstanceListSupplier.builder().withDiscoveryClient()
                .with(hintZone()) // Apply: service-instance header hint strategy
                .build(context)
            return ordered(supplier, context, DEFAULT_REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER)
        }

        /**
         * Used purely to mark in the logs that the reactive branch has been assembled — lets ops
         * see at a glance which LB path is in use.
         *
         * @author K
         * @since 1.0.0
         */
        @PostConstruct
        fun init() {
            LogFactory.getLog(this::class).info("[reactive hint zone preference] initialization complete...")
        }
    }

    /**
     * Wrapper that augments the delegated [ServiceInstanceListSupplier] with a fixed Spring order.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private class OrderedServiceInstanceListSupplier(
        private val delegate: ServiceInstanceListSupplier,
        private val order: Int
    ) : ServiceInstanceListSupplier, Ordered {
        override fun getOrder(): Int = order

        override fun getServiceId(): String = delegate.serviceId

        override fun get(): Flux<MutableList<org.springframework.cloud.client.ServiceInstance>> =
            delegate.get()

        override fun get(request: Request<*>): Flux<MutableList<org.springframework.cloud.client.ServiceInstance>> =
            delegate.get(request)
    }
}
