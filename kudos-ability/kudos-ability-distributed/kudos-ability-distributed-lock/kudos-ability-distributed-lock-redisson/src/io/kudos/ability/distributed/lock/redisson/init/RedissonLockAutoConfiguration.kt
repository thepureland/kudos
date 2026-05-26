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
 * Auto-configuration for the Redisson distributed lock.
 *
 * @author K
 * @author AI: Codex
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
     * Initialize the Redisson connection config.
     *
     * @param properties connection properties
     * @param config     Config
     */
    private fun initRedissonConfig(config: Config, properties: RedissonProperties) {
        // In Redisson 4.0+ the password should be set on the Config object, not on BaseConfig.
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

    /**
     * Copy generic connection parameters (ping/timeout/retry/connection counts, etc.) from properties to Redisson [BaseConfig].
     *
     * Note: since Redisson 4.0+ password has been moved to the top-level [Config] and is no longer part of [BaseConfig],
     * so this method **no longer sets password** — it is handled in [initRedissonConfig] at the top.
     *
     * @param baseConfig single/cluster/sentinel config object ([BaseConfig] is their common base)
     * @param baseConfigProperties caller-side configuration
     * @author K
     * @since 1.0.0
     */
    private fun initBaseConfig(baseConfig: BaseConfig<*>, baseConfigProperties: RedissonBaseConfigProperties) {
        baseConfig.apply {
            pingConnectionInterval = baseConfigProperties.pingConnectionInterval
            idleConnectionTimeout = baseConfigProperties.idleConnectionTimeout
            connectTimeout = baseConfigProperties.connectTimeout
            timeout = baseConfigProperties.timeout
            retryAttempts = baseConfigProperties.retryAttempts
            retryDelay = ConstantDelay(Duration.of(baseConfigProperties.retryInterval.toLong(), ChronoUnit.MILLIS))
            // Note: since Redisson 4.0+ password is set on the Config object (see top of initRedissonConfig);
            // the password setter on BaseConfig was removed during the upgrade, so it is no longer set here.
            subscriptionsPerConnection = baseConfigProperties.subscriptionsPerConnection
            clientName = baseConfigProperties.clientName
        }
    }

    @Bean
    @ConfigurationProperties(prefix = "kudos.ability.distributed.lock.redisson")
    open fun redissonProperties() = RedissonProperties()

    @Bean
    open fun distributedLockAspect() = DistributedLockAspect()

    @Bean(name = [RedissonLockKit.REDISSON_LOCKER_BEAN_NAME])
    open fun redissonLocker(properties: RedissonProperties) =
        RedissonLocker().also {
            RedissonLockKit.setLockKeyPrefix(properties.lockKeyPrefix)
        }

    override fun getComponentName() = "kudos-ability-distributed-lock-redisson"

}
