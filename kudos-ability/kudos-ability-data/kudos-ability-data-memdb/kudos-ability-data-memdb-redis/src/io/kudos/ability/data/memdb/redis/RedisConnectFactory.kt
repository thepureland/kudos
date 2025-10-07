package io.kudos.ability.data.memdb.redis

import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.lettuce.core.ClientOptions
import io.lettuce.core.ReadFrom
import io.lettuce.core.SocketOptions
import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.boot.autoconfigure.data.redis.RedisProperties
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisConfiguration
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import java.time.Duration

/**
 * redis的链接工厂
 */
object RedisConnectFactory {
    /**
     * Lettuce链接工厂
     *
     * @param redisProperties redisProperties
     * @return LettuceConnectionFactory
     */
    fun newLettuceConnectionFactory(redisProperties: RedisExtProperties): LettuceConnectionFactory {
        var clientOptions: ClientOptions? = null
        var redisConfiguration: RedisConfiguration? = null
        //集群模式下的lettuce配置
        if (redisProperties.cluster != null &&
                !redisProperties.cluster.nodes.isNullOrEmpty()
        ) {
            val clusterTopologyRefreshOptions: ClusterTopologyRefreshOptions? = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(60L))
                .enableAdaptiveRefreshTrigger(
                    *arrayOf(
                        ClusterTopologyRefreshOptions.RefreshTrigger.ASK_REDIRECT,
                        ClusterTopologyRefreshOptions.RefreshTrigger.UNKNOWN_NODE
                    )
                )
                .build()

            clientOptions = ClusterClientOptions.builder()
                .topologyRefreshOptions(clusterTopologyRefreshOptions)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .socketOptions(SocketOptions.builder().keepAlive(true).build())
                .validateClusterNodeMembership(false)
                .build()

            redisConfiguration = getClusterRedisConfiguration(redisProperties)
        } else {
            //单机下的lettuce配置
            clientOptions = ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .socketOptions(SocketOptions.builder().keepAlive(true).build())
                .build()
            redisConfiguration = getRedisConfiguration(redisProperties)
        }
        //连接池配置
        val poolConfig = GenericObjectPoolConfig<StatefulConnection<*, *>>()
        poolConfig.maxIdle = redisProperties.maxIdle
        poolConfig.minIdle = redisProperties.minIdle
        poolConfig.setMaxWait(redisProperties.maxWait)
        val maxActive = if (redisProperties.maxActive > 0) redisProperties.maxActive else 200
        poolConfig.maxTotal = maxActive
        //链接配置
        val lettucePoolingClientConfigurationBuilder: LettucePoolingClientConfiguration.LettucePoolingClientConfigurationBuilder =
            LettucePoolingClientConfiguration
                .builder()
                .poolConfig(poolConfig)
                .clientOptions(clientOptions)
                .readFrom(ReadFrom.REPLICA_PREFERRED)

        //note-upgrade-to-spring-3.0-position
        //是否开启ssl
        //if (redisProperties.isSsl()) {
        //    lettucePoolingClientConfigurationBuilder.useSsl();
        //}
        val poolClientConfig = lettucePoolingClientConfigurationBuilder.build()
        val lettuceConnectionFactory = LettuceConnectionFactory(redisConfiguration, poolClientConfig)
        lettuceConnectionFactory.afterPropertiesSet()
        return lettuceConnectionFactory
    }

    /**
     * 单机模式下的redis连接配置
     *
     * @param redisProperties redisProperties
     * @return redisConfiguration
     */
    private fun getRedisConfiguration(redisProperties: RedisProperties): RedisStandaloneConfiguration {
        val config = RedisStandaloneConfiguration()
        config.hostName = redisProperties.host
        config.port = redisProperties.port
        config.database = redisProperties.database
        if (!redisProperties.password.isNullOrBlank()) {
            config.password = RedisPassword.of(redisProperties.password)
        }
        if (!redisProperties.username.isNullOrBlank()) {
            config.username = redisProperties.username
        }
        return config
    }

    /**
     * 集群模式下的redis连接配置
     *
     * @param redisProperties redisProperties
     * @return redisClusterConfiguration
     */
    private fun getClusterRedisConfiguration(redisProperties: RedisProperties): RedisClusterConfiguration {
        //集群模式下链接
        val clusterProperties = redisProperties.cluster
        val config = RedisClusterConfiguration(clusterProperties.nodes)
        if (clusterProperties.maxRedirects != null) {
            config.maxRedirects = clusterProperties.maxRedirects
        }
        if (redisProperties.password != null) {
            config.password = RedisPassword.of(redisProperties.password)
        }
        return config
    }

}
