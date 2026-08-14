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
import org.springframework.data.redis.connection.RedisSentinelConfiguration
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
 *  - Read preference defaults to [ReadFrom.REPLICA_PREFERRED] and is configurable per instance via `read-from`.
 *  - Applies `timeout` (command timeout) and `connect-timeout` from the configuration.
 *  - Enables Lettuce SSL connections when `ssl.enabled=true` or `ssl.bundle` is configured.
 *
 * Deployment mode is picked from the configuration in Spring Boot's own precedence order:
 * `sentinel.nodes` → `cluster.nodes` → standalone.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object RedisConnectFactory {
    /**
     * Creates a [LettuceConnectionFactory] based on configuration; sentinel / cluster / standalone is
     * determined by which of `sentinel.nodes` / `cluster.nodes` is non-empty (in that precedence).
     *
     * @param redisProperties Redis extension configuration
     * @return A connection factory that has already had `afterPropertiesSet()` invoked and can be handed directly to a RedisTemplate.
     */
    fun newLettuceConnectionFactory(redisProperties: RedisExtProperties): LettuceConnectionFactory {
        val isSentinel = !redisProperties.sentinel?.nodes.isNullOrEmpty()
        val isCluster = !isSentinel && !redisProperties.cluster?.nodes.isNullOrEmpty()
        val socketOptionsBuilder = SocketOptions.builder().keepAlive(true)
        redisProperties.connectTimeout?.let { socketOptionsBuilder.connectTimeout(it) }
        val socketOptions = socketOptionsBuilder.build()
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
                .socketOptions(socketOptions)
                .validateClusterNodeMembership(false)
                .build()
        } else {
            ClientOptions.builder()
                .disconnectedBehavior(ClientOptions.DisconnectedBehavior.REJECT_COMMANDS)
                .autoReconnect(true)
                .socketOptions(socketOptions)
                .build()
        }
        val redisConfiguration: RedisConfiguration = when {
            isSentinel -> getSentinelRedisConfiguration(redisProperties)
            isCluster -> getClusterRedisConfiguration(redisProperties)
            else -> getRedisConfiguration(redisProperties)
        }

        val poolConfig = GenericObjectPoolConfig<StatefulConnection<*, *>>().apply {
            maxIdle = redisProperties.maxIdle
            minIdle = redisProperties.minIdle
            setMaxWait(redisProperties.maxWait)
            // commons-pool2 semantics: negative maxTotal = unlimited; passed through as-is.
            maxTotal = redisProperties.maxActive
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
            .readFrom(ReadFrom.valueOf(redisProperties.readFrom))
        // Without this, Lettuce falls back to its 60s default command timeout regardless of configuration.
        redisProperties.timeout?.let { builder.commandTimeout(it) }
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
     * Redis connection configuration for sentinel mode.
     *
     * Two credential pairs are involved and must not be mixed up: the top-level `username` / `password`
     * authenticate against the master/replica data nodes, while `sentinel.username` / `sentinel.password`
     * authenticate against the sentinel nodes themselves.
     *
     * @param redisProperties redisProperties
     * @return redisSentinelConfiguration
     */
    private fun getSentinelRedisConfiguration(redisProperties: DataRedisProperties): RedisSentinelConfiguration {
        val sentinelProperties = checkNotNull(redisProperties.sentinel) { "sentinel must be set when using sentinel mode" }
        val nodes = checkNotNull(sentinelProperties.nodes) { "sentinel.nodes must not be empty" }
        val master = requireNotNull(sentinelProperties.master?.takeIf { it.isNotBlank() }) {
            "sentinel.master must be set when using sentinel mode"
        }
        val config = RedisSentinelConfiguration(master, nodes.toSet())
        config.database = redisProperties.database
        // credentials of the data nodes
        if (!redisProperties.password.isNullOrBlank()) {
            config.password = RedisPassword.of(redisProperties.password)
        }
        if (!redisProperties.username.isNullOrBlank()) {
            config.username = redisProperties.username
        }
        // credentials of the sentinel nodes
        if (!sentinelProperties.password.isNullOrBlank()) {
            config.sentinelPassword = RedisPassword.of(sentinelProperties.password)
        }
        if (!sentinelProperties.username.isNullOrBlank()) {
            config.sentinelUsername = sentinelProperties.username
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
        if (!redisProperties.password.isNullOrBlank()) {
            config.password = RedisPassword.of(redisProperties.password)
        }
        if (!redisProperties.username.isNullOrBlank()) {
            config.username = redisProperties.username
        }
        return config
    }

}
