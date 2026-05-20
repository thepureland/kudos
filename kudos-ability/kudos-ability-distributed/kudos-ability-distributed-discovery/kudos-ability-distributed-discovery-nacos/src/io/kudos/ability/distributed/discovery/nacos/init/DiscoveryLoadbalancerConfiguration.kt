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
 * 自定义负载策略：当 `spring.cloud.loadbalancer.configurations=zone-preference` 时启用
 * [HintZoneServiceInstanceListSupplier]——按客户端请求的 `hint header` 选目标实例 zone。
 *
 * 同时支持 blocking 和 reactive 两种 discovery 客户端；通过条件注解按需装配。
 *
 * @see com.alibaba.cloud.nacos.loadbalancer.NacosLoadBalancerClientConfiguration
 * @author K
 * @since 1.0.0
 */
@Configuration
@LoadBalancerClients(defaultConfiguration = [DiscoveryLoadbalancerConfiguration::class])
open class DiscoveryLoadbalancerConfiguration {


    companion object {
        /**
         * Spring Cloud 内部 `ServiceInstanceListSupplier` 的 Order 常量约定值——必须比
         * Spring Cloud 默认实现优先级低一点，让 Spring Cloud 内置 supplier 先注册再被本类覆盖。
         * 选这个具体数字是历史遗留（保持与原 `NacosLoadBalancerClientConfiguration` 一致）。
         */
        private const val REACTIVE_SERVICE_INSTANCE_SUPPLIER_ORDER = 183827465

        private const val ZONE_METADATA_KEY_PROPERTY = "kudos.ability.distributed.discovery.nacos.zone-metadata-key"

        private fun hintZone(): ServiceInstanceListSupplierBuilder.DelegateCreator {
            return ServiceInstanceListSupplierBuilder.DelegateCreator { context: ConfigurableApplicationContext, delegate: ServiceInstanceListSupplier ->
                val loadBalancerClientFactory: LoadBalancerClientFactory = context.getBean<LoadBalancerClientFactory>()
                val zoneConfig: LoadBalancerZoneConfig = context.getBean(LoadBalancerZoneConfig::class.java)
                // 可配置 metadata 字段名——nacos 实例上挂的是 region / cluster-zone 等场景；
                // 缺省回退到 spring-cloud-loadbalancer 的标准 "zone"
                val zoneMetadataKey = context.environment.getProperty(ZONE_METADATA_KEY_PROPERTY)
                    ?.takeIf { it.isNotBlank() }
                    ?: HintZoneServiceInstanceListSupplier.DEFAULT_ZONE_METADATA_KEY
                HintZoneServiceInstanceListSupplier(delegate, zoneConfig, loadBalancerClientFactory, zoneMetadataKey)
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

        /**
         * 仅用于在日志里标记 blocking 分支已装配——便于运维一眼确认走哪条 LB 路径。
         *
         * @author K
         * @since 1.0.0
         */
        @PostConstruct
        fun init() {
            LogFactory.getLog(this::class).info("[blocking hint zone preference]初始化完成...")
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

        /**
         * 仅用于在日志里标记 reactive 分支已装配——便于运维一眼确认走哪条 LB 路径。
         *
         * @author K
         * @since 1.0.0
         */
        @PostConstruct
        fun init() {
            LogFactory.getLog(this::class).info("[reactive hint zone preference]初始化完成...")
        }
    }
}
