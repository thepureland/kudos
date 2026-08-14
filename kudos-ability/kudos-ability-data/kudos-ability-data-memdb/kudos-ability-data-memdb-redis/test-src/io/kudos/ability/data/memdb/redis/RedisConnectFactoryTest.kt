package io.kudos.ability.data.memdb.redis

import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.lettuce.core.ClientOptions
import io.lettuce.core.ReadFrom
import io.lettuce.core.api.StatefulConnection
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import java.time.Duration
import org.springframework.boot.data.redis.autoconfigure.DataRedisProperties
import org.springframework.data.redis.connection.RedisPassword
import org.springframework.data.redis.connection.RedisStandaloneConfiguration
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory
import org.springframework.data.redis.connection.lettuce.LettucePoolingClientConfiguration
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisConnectFactory] client connection configuration.
 *
 * Also covers [RedisConnectFactory.newLettuceConnectionFactory] for both standalone and cluster
 * mode: the factory is built lazily (no connection is opened), so the standalone / cluster
 * configuration, auth and pool branches can be asserted offline without a real Redis.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedisConnectFactoryTest {

    /**
     * Since spring-data-redis 4.1, `LettuceConnectionFactory.getStandaloneConfiguration()` returns the internal
     * default configuration (localhost:6379) when the factory was constructed with a statically-typed
     * [org.springframework.data.redis.connection.RedisConfiguration]; the actual configuration lives in the
     * private `configuration` field, which this helper extracts.
     */
    private fun standaloneConfigOf(factory: LettuceConnectionFactory): RedisStandaloneConfiguration {
        val field = LettuceConnectionFactory::class.java.getDeclaredField("configuration")
        field.isAccessible = true
        return field.get(factory) as RedisStandaloneConfiguration
    }

    // ---------- newLettuceConnectionFactory: standalone ----------

    @Test
    fun newLettuceConnectionFactory_standalone_withAuthAndExplicitPool() {
        val properties = RedisExtProperties().apply {
            host = "redis.example.local"
            port = 16379
            database = 2
            password = "s3cret"
            username = "admin"
            maxActive = 5
            maxIdle = 4
            minIdle = 1
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            assertFalse(factory.isClusterAware)
            // public getters read the actual configuration object regardless of its static type
            assertEquals("redis.example.local", factory.hostName)
            assertEquals(16379, factory.port)
            assertEquals(2, factory.database)
            val standalone = standaloneConfigOf(factory)
            assertEquals(RedisPassword.of("s3cret"), standalone.password)
            assertEquals("admin", standalone.username)
            val clientConfig = factory.clientConfiguration as LettucePoolingClientConfiguration
            assertEquals(5, clientConfig.poolConfig.maxTotal)
            assertEquals(4, clientConfig.poolConfig.maxIdle)
            assertEquals(1, clientConfig.poolConfig.minIdle)
            assertFalse(clientConfig.isUseSsl)
        } finally {
            factory.destroy()
        }
    }

    @Test
    fun newLettuceConnectionFactory_standalone_withoutAuth_negativeMaxActiveMeansUnlimited() {
        val properties = RedisExtProperties().apply {
            host = "localhost"
            port = 6379
            password = null
            username = null
            maxActive = -1 // commons-pool2 semantics: negative = no limit, passed through as-is
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            assertFalse(factory.isClusterAware)
            val standalone = standaloneConfigOf(factory)
            assertEquals(RedisPassword.none(), standalone.password)
            assertNull(standalone.username)
            val clientConfig = factory.clientConfiguration as LettucePoolingClientConfiguration
            assertEquals(-1, clientConfig.poolConfig.maxTotal)
        } finally {
            factory.destroy()
        }
    }

    // ---------- newLettuceConnectionFactory: sentinel ----------

    @Test
    fun newLettuceConnectionFactory_sentinel_withSeparateDataAndSentinelCredentials() {
        val properties = RedisExtProperties().apply {
            sentinel = DataRedisProperties.Sentinel().apply {
                master = "mymaster"
                nodes = listOf("127.0.0.1:26379", "127.0.0.1:26380")
                username = "sentinelUser"
                password = "sentinelPw"
            }
            database = 3
            username = "dataUser"
            password = "dataPw"
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            val sentinelConfig = assertNotNull(factory.sentinelConfiguration)
            assertEquals("mymaster", sentinelConfig.master!!.name)
            assertEquals(2, sentinelConfig.sentinels.size)
            assertEquals(3, sentinelConfig.database)
            // the two credential pairs must not be swapped: top-level auth is for the data nodes...
            assertEquals(RedisPassword.of("dataPw"), sentinelConfig.password)
            assertEquals("dataUser", sentinelConfig.username)
            // ...and sentinel.* auth is for the sentinel nodes themselves
            assertEquals(RedisPassword.of("sentinelPw"), sentinelConfig.sentinelPassword)
            assertEquals("sentinelUser", sentinelConfig.sentinelUsername)
        } finally {
            factory.destroy()
        }
    }

    @Test
    fun newLettuceConnectionFactory_sentinel_takesPrecedenceOverCluster() {
        // Spring Boot's own precedence: sentinel wins when both are configured
        val properties = RedisExtProperties().apply {
            sentinel = DataRedisProperties.Sentinel().apply {
                master = "mymaster"
                nodes = listOf("127.0.0.1:26379")
            }
            cluster = DataRedisProperties.Cluster().apply { nodes = listOf("127.0.0.1:7000") }
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            assertNotNull(factory.sentinelConfiguration)
            assertFalse(factory.isClusterAware)
        } finally {
            factory.destroy()
        }
    }

    @Test
    fun newLettuceConnectionFactory_sentinel_withoutMaster_failsFast() {
        val properties = RedisExtProperties().apply {
            sentinel = DataRedisProperties.Sentinel().apply {
                nodes = listOf("127.0.0.1:26379")
            }
        }
        val ex = assertFailsWith<IllegalArgumentException> {
            RedisConnectFactory.newLettuceConnectionFactory(properties)
        }
        assertTrue(ex.message!!.contains("sentinel.master"), ex.message)
    }

    @Test
    fun newLettuceConnectionFactory_emptySentinelNodes_fallsBackToStandalone() {
        val properties = RedisExtProperties().apply {
            sentinel = DataRedisProperties.Sentinel().apply { nodes = emptyList() }
            host = "localhost"
            port = 6379
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            assertNull(factory.sentinelConfiguration)
            assertFalse(factory.isClusterAware)
        } finally {
            factory.destroy()
        }
    }

    @Test
    fun newLettuceConnectionFactory_standalone_blankPassword_isIgnored() {
        val properties = RedisExtProperties().apply {
            host = "localhost"
            port = 6379
            password = "  "
            username = ""
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            val standalone = standaloneConfigOf(factory)
            assertEquals(RedisPassword.none(), standalone.password)
            assertNull(standalone.username)
        } finally {
            factory.destroy()
        }
    }

    // ---------- newLettuceConnectionFactory: cluster ----------

    @Test
    fun newLettuceConnectionFactory_cluster_withMaxRedirectsAndAuth() {
        val properties = RedisExtProperties().apply {
            cluster = DataRedisProperties.Cluster().apply {
                nodes = listOf("127.0.0.1:7000", "127.0.0.1:7001")
                maxRedirects = 3
            }
            password = "clusterPw"
            username = "clusterUser" // Redis 6+ ACL user must reach the cluster configuration too
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            assertTrue(factory.isClusterAware)
            val clusterConfig = assertNotNull(factory.clusterConfiguration)
            assertEquals(2, clusterConfig.clusterNodes.size)
            assertEquals(3, clusterConfig.maxRedirects)
            assertEquals(RedisPassword.of("clusterPw"), clusterConfig.password)
            assertEquals("clusterUser", clusterConfig.username)
        } finally {
            factory.destroy()
        }
    }

    @Test
    fun newLettuceConnectionFactory_cluster_withoutOptionalSettings() {
        val properties = RedisExtProperties().apply {
            cluster = DataRedisProperties.Cluster().apply {
                nodes = listOf("127.0.0.1:7000")
            }
            password = null
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            assertTrue(factory.isClusterAware)
            val clusterConfig = assertNotNull(factory.clusterConfiguration)
            assertEquals(1, clusterConfig.clusterNodes.size)
            assertEquals(RedisPassword.none(), clusterConfig.password)
        } finally {
            factory.destroy()
        }
    }

    @Test
    fun newLettuceConnectionFactory_emptyClusterNodes_fallsBackToStandalone() {
        val properties = RedisExtProperties().apply {
            cluster = DataRedisProperties.Cluster().apply { nodes = emptyList() }
            host = "localhost"
            port = 6379
        }
        val factory = RedisConnectFactory.newLettuceConnectionFactory(properties)
        try {
            assertFalse(factory.isClusterAware)
        } finally {
            factory.destroy()
        }
    }

    @Test
    fun newLettuceClientConfiguration_disablesSslByDefault() {
        val config = RedisConnectFactory.newLettuceClientConfiguration(
            RedisExtProperties(),
            ClientOptions.builder().build(),
            GenericObjectPoolConfig<StatefulConnection<*, *>>()
        )

        assertFalse(config.isUseSsl)
    }

    @Test
    fun newLettuceClientConfiguration_enablesSslWhenConfigured() {
        val properties = RedisExtProperties().apply {
            ssl.setEnabled(true)
        }

        val config = RedisConnectFactory.newLettuceClientConfiguration(
            properties,
            ClientOptions.builder().build(),
            GenericObjectPoolConfig<StatefulConnection<*, *>>()
        )

        assertTrue(config.isUseSsl)
    }

    @Test
    fun newLettuceClientConfiguration_enablesSslWhenBundleConfigured() {
        val properties = RedisExtProperties().apply {
            ssl.bundle = "redis-client"
        }

        val config = RedisConnectFactory.newLettuceClientConfiguration(
            properties,
            ClientOptions.builder().build(),
            GenericObjectPoolConfig<StatefulConnection<*, *>>()
        )

        assertTrue(config.isUseSsl)
    }

    @Test
    fun newLettuceClientConfiguration_appliesCommandTimeout() {
        val properties = RedisExtProperties().apply {
            timeout = Duration.ofSeconds(3)
        }

        val config = RedisConnectFactory.newLettuceClientConfiguration(
            properties,
            ClientOptions.builder().build(),
            GenericObjectPoolConfig<StatefulConnection<*, *>>()
        )

        assertEquals(Duration.ofSeconds(3), config.commandTimeout)
    }

    @Test
    fun newLettuceClientConfiguration_readFrom_defaultsToReplicaPreferred_andIsConfigurable() {
        val defaultConfig = RedisConnectFactory.newLettuceClientConfiguration(
            RedisExtProperties(),
            ClientOptions.builder().build(),
            GenericObjectPoolConfig<StatefulConnection<*, *>>()
        )
        assertEquals(ReadFrom.REPLICA_PREFERRED, defaultConfig.readFrom.get())

        val anyConfig = RedisConnectFactory.newLettuceClientConfiguration(
            RedisExtProperties().apply { readFrom = "any" },
            ClientOptions.builder().build(),
            GenericObjectPoolConfig<StatefulConnection<*, *>>()
        )
        assertEquals(ReadFrom.ANY, anyConfig.readFrom.get())
    }
}
