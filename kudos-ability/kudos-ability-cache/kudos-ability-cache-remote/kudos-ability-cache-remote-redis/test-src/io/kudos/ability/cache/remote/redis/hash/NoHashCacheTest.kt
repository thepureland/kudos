package io.kudos.ability.cache.remote.redis.hash

import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNotSame

/**
 * Tests with hash caching disabled (modeled after [io.kudos.ability.cache.remote.redis.keyvalue.NoCacheTest]).
 * When caching is off: MixHashCacheManager is not injected, getHashCache throws, and Hash-annotated methods
 * execute on every call and return different results.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@EnableKudosTest
@Import(HashCacheableTestService::class)
internal class NoHashCacheTest {

    @Autowired
    private lateinit var hashCacheableTestService: HashCacheableTestService

    @Autowired(required = false)
    private lateinit var mixHashCacheManager: MixHashCacheManager

    companion object {
        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "false" }
        }
    }

    @Test
    fun testNoHashCache() {
        assertFailsWith<UninitializedPropertyAccessException> { mixHashCacheManager }
        assertFailsWith<IllegalStateException> { HashCacheKit.getHashCache("testHash") }

        val key = "key"
        hashCacheableTestService.putTestData(key, TestRow(id = key, name = "NoCache", type = 1))
        val value1 = hashCacheableTestService.getTestRowById(key)
        val value2 = hashCacheableTestService.getTestRowById(key)
        assertNotNull(value1)
        assertNotNull(value2)
        assertNotSame(value1, value2)
    }
}
