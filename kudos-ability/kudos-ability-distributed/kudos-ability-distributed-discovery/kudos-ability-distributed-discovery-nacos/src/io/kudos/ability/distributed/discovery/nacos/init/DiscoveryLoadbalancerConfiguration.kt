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
import org.springframework.cloud.loadbalancer.annotation.LoadBalancerClients
import org.springframework.cloud.loadbalancer.config.LoadBalancerZoneConfig
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplier
import org.springframework.cloud.loadbalancer.core.ServiceInstanceListSupplierBuilder
import org.springframework.cloud.loadbalancer.support.LoadBalancerClientFactory
import org.springframework.context.ConfigurableApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.core.annotation.Order

/**
 * 自定义负载策略
 * @see com.alibaba.cloud.nacos.loadbalancer.NacosLoadBalancerClientConfiguration
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = [DiscoveryLoadbalancerConfiguration::class])
open class DiscoveryLoadbalancerConfiguration {


    companion object {
        private const val REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 183827465

        private fun hintZone(): ServiceInstanceListSupplierBuilder.DelegateCreator {
            return ServiceInstanceListSupplierBuilder.DelegateCreator { context: ConfigurableApplicationContext, delegate: ServiceInstanceListSupplier ->
                val loadBalancerClientFactory: LoadBalancerClientFactory = context.getBean<LoadBalancerClientFactory>()
                val zoneConfig: LoadBalancerZoneConfig = context.getBean(LoadBalancerZoneConfig::class.java)
                HintZoneServiceInstanceListSupplier(delegate, zoneConfig, loadBalancerClientFactory)
            }
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnBlockingDiscoveryEnabled
    @Order(REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER - 2)
    open class BlockingSupportConfiguration {
        @Bean
        @ConditionalOnBean(DiscoveryClient::class) //@ConditionalOnMissingBean
        @ConditionalOnProperty(value = ["spring.cloud.loadbalancer.configurations"], havingValue = "zone-preference")
        open fun zonePreferenceDiscoveryClientServiceInstanceListSupplier(
            context: ConfigurableApplicationContext
        ): ServiceInstanceListSupplier? {
            return ServiceInstanceListSupplier.builder().withBlockingDiscoveryClient()
                .with(hintZone()) //应用: 服务实例 Header提示策略
                .build(context)
        }

        @PostConstruct
        fun init() {
            LogFactory.getLog(this).info("[blocking hint zone preference]初始化完成...")
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnReactiveDiscoveryEnabled
    @Order(
        REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER
    )
    open class ReactiveSupportConfiguration {
        @Bean
        @ConditionalOnBean(ReactiveDiscoveryClient::class) //@ConditionalOnMissingBean
        @ConditionalOnProperty(value = ["spring.cloud.loadbalancer.configurations"], havingValue = "zone-preference")
        open fun zonePreferenceDiscoveryClientServiceInstanceListSupplier(
            context: ConfigurableApplicationContext
        ): ServiceInstanceListSupplier? {
            return ServiceInstanceListSupplier.builder().withDiscoveryClient()
                .with(hintZone()) //应用: 服务实例 Header提示策略
                .build(context)
        }

        @PostConstruct
        fun init() {
            LogFactory.getLog(this).info("[reactive hint zone preference]初始化完成...")
        }
    }
}
