package io.kudos.ability.data.memdb.redis

import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.lettuce.core.ClientOptions
import io.lettuce.core.ReadFrom
import io.lettuce.core.SocketOptions
import io.lettuce.core.api.StatefulConnection
import io.lettuce.core.cluster.ClusterClientOptions
import io.lettuce.core.cluster.ClusterTopologyRefreshOptions
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.data.redis.connection.RedisClusterConfiguration
import org.springframework.data.redis.connection.RedisConfiguration
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import java.time.Duration

/**
 * Redis connection factory. Responsible for constructing the Spring Data Redis [LettuceConnectionFactory],
 * automatically choosing between cluster and standalone mode based on whether [RedisExtProperties.cluster] is configured.
 *
 * Differences from Spring Boot's default `RedisAutoConfiguration`:
 *  - Directly reads the kudos custom [RedisExtProperties], supporting independent connection pool parameters per Redis instance.
 *  - Enables keepAlive and autoReconnect by default, with additional periodic topology refresh in cluster mode.
 *  - Defaults to [ReadFrom.REPLICA_PREFERRED] so read requests prefer replica nodes.
 *  - Enables Lettuce SSL connections when `ssl.enabled=true` or `ssl.bundle` is configured.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object RedisConnectFactory {
    /**
     * Creates a [LettuceConnectionFactory] based on configuration; cluster vs standalone is determined by whether `cluster.nodes` is non-empty.
     *
     * @param redisProperties Redis extension configuration
     * @return A connection factory that has already had `afterPropertiesSet()` invoked and can be handed directly to a RedisTemplate.
     */
    fun newLettuceConnectionFactory(redisProperties: RedisExtProperties): LettuceConnectionFactory {
        val isCluster = !redisProperties.cluster?.nodes.isNullOrEmpty()
        val clientOptions: ClientOptions = if (isCluster) {
            val refreshOptions = ClusterTopologyRefreshOptions.builder()
                .enablePeriodicRefresh(Duration.ofSeconds(60L))
                .enableAdaptiveRefreshTrigger(
                    ClusterTopologyRefreshOptions.RefreshTrigger.ASK_REDIRECT,
                    ClusterTopologyRefreshOptions.RefreshTrigger.UNKNOWN_NODE
                )
                .build()
            ClusterClientOptions.builder()
                .topologyRefreshOptions(refreshOptions)
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .socketOptions(SocketOptions.builder().keepAlive(true).build())
                .validateClusterNodeMembership(false)
                .build()
        } else {
            ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .socketOptions(SocketOptions.builder().keepAlive(true).build())
                .build()
        }
        val redisConfiguration: RedisConfiguration =
            if (isCluster) getClusterRedisConfiguration(redisProperties)
            else getRedisConfiguration(redisProperties)

        val poolConfig = GenericObjectPoolConfig<StatefulConnection<*, *>>().apply {
            maxIdle = redisProperties.maxIdle
            minIdle = redisProperties.minIdle
            setMaxWait(redisProperties.maxWait)
            maxTotal = if (redisProperties.maxActive > 0) redisProperties.maxActive else 200
        }

        val poolClientConfig = newLettuceClientConfiguration(redisProperties, clientOptions, poolConfig)

        return LettuceConnectionFactory(redisConfiguration, poolClientConfig).apply {
            afterPropertiesSet()
        }
    }

    /**
     * Creates the Lettuce client configuration; extracted into a separate method to make it easy to cover SSL / pool branches that do not require a real Redis.
     */
    internal fun newLettuceClientConfiguration(
        redisProperties: RedisExtProperties,
        clientOptions: ClientOptions,
        poolConfig: GenericObjectPoolConfig<StatefulConnection<*, *>>
    ): LettucePoolingClientConfiguration {
        val builder = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .clientOptions(clientOptions)
            .readFrom(ReadFrom.REPLICA_PREFERRED)
        if (redisProperties.ssl.isEnabled) {
            builder.useSsl()
        }
        return builder.build()
    }

    /**
     * Redis connection configuration for standalone mode.
     *
     * @param redisProperties redisProperties
     * @return redisConfiguration
     */
    private fun getRedisConfiguration(redisProperties: DataRedisProperties): RedisStandaloneConfiguration {
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
     * Redis connection configuration for cluster mode.
     *
     * @param redisProperties redisProperties
     * @return redisClusterConfiguration
     */
    private fun getClusterRedisConfiguration(redisProperties: DataRedisProperties): RedisClusterConfiguration {
        val clusterProperties = checkNotNull(redisProperties.cluster) { "cluster must be set when using cluster mode" }
        val nodes = checkNotNull(clusterProperties.nodes) { "cluster.nodes must not be empty" }
        val config = RedisClusterConfiguration(nodes)
        clusterProperties.maxRedirects?.let { config.setMaxRedirects(it) }
        if (redisProperties.password != null) {
            config.password = RedisPassword.of(redisProperties.password)
        }
        return config
    }

}
