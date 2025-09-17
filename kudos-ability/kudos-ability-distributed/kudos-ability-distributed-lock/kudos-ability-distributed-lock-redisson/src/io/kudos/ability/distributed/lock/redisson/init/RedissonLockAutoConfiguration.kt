package io.kudos.ability.distributed.lock.redisson.init

import io.kudos.context.init.ContextAutoConfiguration
import io.kudos.context.init.IComponentInitializer
import io.kudos.context.config.YamlPropertySourceFactory
import org.redisson.Redisson
import org.redisson.api.RedissonClient
import org.redisson.config.BaseConfig
import org.redisson.config.Config
import org.redisson.config.ReadMode
import org.redisson.config.TransportMode
import org.soul.ability.distributed.lock.redisson.RedissonLockTool
import org.soul.ability.distributed.lock.redisson.annotations.DistributedLockAspect
import org.soul.ability.distributed.lock.redisson.locker.RedissonLocker
import org.soul.ability.distributed.lock.redisson.starter.properties.RedissonBaseConfigProperties
import org.soul.ability.distributed.lock.redisson.starter.properties.RedissonProperties
import org.soul.base.lang.string.StringTool
import org.springframework.boot.autoconfigure.AutoConfigureAfter
import org.springframework.boot.context.properties.ConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource

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

    @Bean(name = [RedissonLockTool.REDISSON_CLIENT_BEAN_NAME], destroyMethod = "shutdown")
    open fun redisson(properties: RedissonProperties): RedissonClient? {
        if (!properties.enabled) {
            return null
        }
        val config = Config()
        config.setNettyThreads(properties.getConfig().getNettyThreads())
        config.setThreads(properties.getConfig().getThreads())
        config.setTransportMode(TransportMode.valueOf(properties.getConfig().getTransportMode()))

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
        when (properties.mode) {
            "single" -> {
                val singleServerConfig = config.useSingleServer()
                singleServerConfig.subscriptionConnectionMinimumIdleSize = properties.baseConfig.subscriptionConnectionMinimumIdleSize
                singleServerConfig.subscriptionConnectionPoolSize = properties.baseConfig.subscriptionConnectionPoolSize
                singleServerConfig.dnsMonitoringInterval = properties.baseConfig.dnsMonitoringInterval
                this.initBaseConfig(singleServerConfig, properties.baseConfig)
                singleServerConfig.address = properties.singleServerConfig.address
                singleServerConfig.connectionMinimumIdleSize = properties.singleServerConfig.connectionMinimumIdleSize
                singleServerConfig.connectionPoolSize = properties.singleServerConfig.connectionPoolSize
                singleServerConfig.database = properties.singleServerConfig.database
            }

            "cluster" -> {
                val clusterServersConfig = config.useClusterServers()
                clusterServersConfig.subscriptionConnectionMinimumIdleSize = properties.baseConfig.subscriptionConnectionMinimumIdleSize
                clusterServersConfig.subscriptionConnectionPoolSize = properties.baseConfig.subscriptionConnectionPoolSize
                clusterServersConfig.dnsMonitoringInterval = properties.baseConfig.dnsMonitoringInterval
                this.initBaseConfig(clusterServersConfig, properties.baseConfig)
                clusterServersConfig.slaveConnectionMinimumIdleSize = properties.clusterServersConfig.slaveConnectionMinimumIdleSize
                clusterServersConfig.slaveConnectionPoolSize = properties.clusterServersConfig.slaveConnectionPoolSize
                clusterServersConfig.masterConnectionMinimumIdleSize = properties.clusterServersConfig.masterConnectionMinimumIdleSize
                clusterServersConfig.masterConnectionPoolSize = properties.clusterServersConfig.masterConnectionPoolSize
                clusterServersConfig.readMode = ReadMode.valueOf(properties.clusterServersConfig.readMode)
                clusterServersConfig.scanInterval = properties.clusterServersConfig.scanInterval
                val nodeAddresses = properties.clusterServersConfig.nodeAddresses
                clusterServersConfig.addNodeAddress(*nodeAddresses)
            }

            else -> {}
        }
    }

    private fun initBaseConfig(baseConfig: BaseConfig<*>, baseConfigProperties: RedissonBaseConfigProperties) {
        baseConfig.pingConnectionInterval = baseConfigProperties.pingConnectionInterval
        baseConfig.idleConnectionTimeout = baseConfigProperties.idleConnectionTimeout
        baseConfig.connectTimeout = baseConfigProperties.connectTimeout
        baseConfig.timeout = baseConfigProperties.timeout
        baseConfig.retryAttempts = baseConfigProperties.retryAttempts
        baseConfig.retryInterval = baseConfigProperties.retryInterval
        if (StringTool.isNotBlank(baseConfigProperties.password)) {
            baseConfig.password = baseConfigProperties.password
        }
        baseConfig.subscriptionsPerConnection = baseConfigProperties.subscriptionsPerConnection
        baseConfig.clientName = baseConfigProperties.clientName
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
