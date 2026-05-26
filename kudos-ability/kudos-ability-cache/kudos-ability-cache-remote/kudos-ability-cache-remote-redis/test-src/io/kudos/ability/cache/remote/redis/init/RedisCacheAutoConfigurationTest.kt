package io.kudos.ability.cache.remote.redis.init

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [RedisCacheAutoConfiguration] configuration behavior.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class RedisCacheAutoConfigurationTest {

    @Test
    fun cacheNodeId_usesConfiguredStableNodeId() {
        val properties = RedisCacheProperties().apply {
            nodeId = "  host-a:12345  "
        }

        val nodeId = RedisCacheAutoConfiguration().cacheNodeId(properties)

        assertEquals("host-a:12345", nodeId)
    }

    @Test
    fun cacheNodeId_generatesUuidWhenNodeIdBlank() {
        val config = RedisCacheAutoConfiguration()
        val first = config.cacheNodeId(RedisCacheProperties().apply { nodeId = " " })
        val second = config.cacheNodeId(RedisCacheProperties())

        assertTrue(first.isNotBlank())
        assertTrue(second.isNotBlank())
        assertNotEquals(first, second)
    }
}
