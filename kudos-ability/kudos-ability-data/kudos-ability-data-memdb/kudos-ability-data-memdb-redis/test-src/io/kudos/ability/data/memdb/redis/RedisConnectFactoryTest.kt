package io.kudos.ability.data.memdb.redis

import io.kudos.ability.data.memdb.redis.init.properties.RedisExtProperties
import io.lettuce.core.ClientOptions
import io.lettuce.core.api.StatefulConnection
import org.apache.commons.pool2.impl.GenericObjectPoolConfig
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisConnectFactory] client connection configuration.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedisConnectFactoryTest {

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
}
