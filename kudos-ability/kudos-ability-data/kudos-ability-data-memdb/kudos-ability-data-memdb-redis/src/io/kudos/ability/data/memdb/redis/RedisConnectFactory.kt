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
 * Redis 连接工厂。负责构造 Spring Data Redis 的 [LettuceConnectionFactory]，
 * 内部根据 [RedisExtProperties.cluster] 是否配置自动决定走集群 vs 单机模式。
 *
 * 区别于 Spring Boot 默认 `RedisAutoConfiguration`：
 *  - 直接读取 kudos 自定义的 [RedisExtProperties]，支持每个 redis 实例独立的连接池参数
 *  - 默认开启 keepAlive、autoReconnect，集群模式下额外开启周期性 topology 刷新
 *  - 默认 [ReadFrom.REPLICA_PREFERRED]，读请求优先打从节点
 *
 * SSL 暂未接入——`DataRedisProperties` 上有 ssl 配置，需要时在此处装 `.useSsl()`。
 *
 * @author K
 * @since 1.0.0
 */
object RedisConnectFactory {
    /**
     * 根据配置创建 [LettuceConnectionFactory]；集群 vs 单机由 `cluster.nodes` 是否非空决定。
     *
     * @param redisProperties Redis 扩展配置
     * @return 已经 `afterPropertiesSet()` 过、可直接交给 RedisTemplate 的连接工厂
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

        val poolClientConfig = LettucePoolingClientConfiguration.builder()
            .poolConfig(poolConfig)
            .clientOptions(clientOptions)
            .readFrom(ReadFrom.REPLICA_PREFERRED)
            .build()

        return LettuceConnectionFactory(redisConfiguration, poolClientConfig).apply {
            afterPropertiesSet()
        }
    }

    /**
     * 单机模式下的redis连接配置
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
     * 集群模式下的redis连接配置
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
