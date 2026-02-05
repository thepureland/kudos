package io.kudos.ability.distributed.lock.redisson.init

import io.kudos.ability.distributed.lock.redisson.annotations.DistributedLockAspect
import io.kudos.ability.distributed.lock.redisson.bean.RedissonLockProvider
import io.kudos.ability.distributed.lock.redisson.init.properties.RedissonBaseConfigProperties
import io.kudos.ability.distributed.lock.redisson.init.properties.RedissonProperties
import io.kudos.ability.distributed.lock.redisson.kit.RedissonLockKit
import io.kudos.ability.distributed.lock.redisson.locker.RedissonLocker
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.*
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import java.time.Duration
import java.time.temporal.ChronoUnit

/**
 * redisson分布式锁自动配置类
 *
 * @author K
 * @since 1.0.0
 */
@Configuration
@AutoConfigureAfter(ContextAutoConfiguration::class)
@PropertySource(
    value = ["classpath:kudos-ability-distributed-lock-redisson.yml"],
    factory = YamlPropertySourceFactory::class
)
open class RedissonLockAutoConfiguration : IComponentInitializer {


    @Bean
    @ConditionalOnMissingBean
    open fun redissonLockProvider() = RedissonLockProvider()

    @Bean(name = [RedissonLockKit.REDISSON_CLIENT_BEAN_NAME], destroyMethod = "shutdown")
    @ConditionalOnMissingBean
    open fun redissonClient(properties: RedissonProperties): RedissonClient? {
        if (!properties.enabled) {
            return null
        }
        val config = Config()
        properties.config?.let {
            config.apply {
                nettyThreads = it.nettyThreads
                threads = it.threads
                transportMode = TransportMode.valueOf(it.transportMode)
            }
        }

        initRedissonConfig(config, properties)
        return Redisson.create(config)
    }

    /**
     * 初始化Redisson连接配置
     *
     * @param properties 连接配置文件
     * @param config     Config
     */
    private fun initRedissonConfig(config: Config, properties: RedissonProperties) {
        // Redisson 4.0+ 中，密码应该在 Config 对象上设置，而不是在 BaseConfig 上
        properties.baseConfig?.let {
            if (!it.password.isNullOrBlank()) {
                config.setPassword(it.password)
            }
        }

        when (properties.mode) {
            "single" -> {
                config.useSingleServer().apply {
                    properties.baseConfig?.let {
                        subscriptionConnectionMinimumIdleSize = it.subscriptionConnectionMinimumIdleSize
                        subscriptionConnectionPoolSize = it.subscriptionConnectionPoolSize
                        dnsMonitoringInterval = it.dnsMonitoringInterval
                        initBaseConfig(this, it)
                    }
                    properties.singleServerConfig?.let {
                        address = it.address
                        connectionMinimumIdleSize = it.connectionMinimumIdleSize
                        connectionPoolSize = it.connectionPoolSize
                        database = it.database
                    }
                }
            }

            "cluster" -> {
                config.useClusterServers().apply {
                    properties.baseConfig?.let {
                        subscriptionConnectionMinimumIdleSize = it.subscriptionConnectionMinimumIdleSize
                        subscriptionConnectionPoolSize = it.subscriptionConnectionPoolSize
                        dnsMonitoringInterval = it.dnsMonitoringInterval
                        initBaseConfig(this, it)
                    }
                    properties.clusterServersConfig?.let {
                        slaveConnectionMinimumIdleSize = it.slaveConnectionMinimumIdleSize
                        slaveConnectionPoolSize = it.slaveConnectionPoolSize
                        masterConnectionMinimumIdleSize = it.masterConnectionMinimumIdleSize
                        masterConnectionPoolSize = it.masterConnectionPoolSize
                        readMode = ReadMode.valueOf(it.readMode)
                        scanInterval = it.scanInterval
                        val nodeAddresses = it.nodeAddresses
                        addNodeAddress(*nodeAddresses)
                    }
                }
            }

            else -> {}
        }
    }

    private fun initBaseConfig(baseConfig: BaseConfig<*>, baseConfigProperties: RedissonBaseConfigProperties) {
        baseConfig.apply {
            pingConnectionInterval = baseConfigProperties.pingConnectionInterval
            idleConnectionTimeout = baseConfigProperties.idleConnectionTimeout
            connectTimeout = baseConfigProperties.connectTimeout
            timeout = baseConfigProperties.timeout
            retryAttempts = baseConfigProperties.retryAttempts
            retryDelay = ConstantDelay(Duration.of(baseConfigProperties.retryInterval.toLong(), ChronoUnit.MILLIS))
//            if (!baseConfigProperties.password.isNullOrBlank()) {
//                setPassword(baseConfigProperties.password)
//            }
            subscriptionsPerConnection = baseConfigProperties.subscriptionsPerConnection
            clientName = baseConfigProperties.clientName
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.lock.redisson")
    open fun redissonProperties() = RedissonProperties()

    @Bean
    open fun distributedLockAspect() = DistributedLockAspect()

    @Bean
    open fun redissonLocker() = RedissonLocker()

    override fun getComponentName() = "kudos-ability-distributed-lock-redisson"

}
