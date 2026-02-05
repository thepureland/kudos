package io.kudos.ability.cache.local.caffeine.hash

import io.kudos.ability.cache.common.core.hash.MixHashCacheManager
import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.test.common.init.EnableKudosTest
import org.junit.jupiter.api.assertThrows
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertNotEquals

/**
 * 不启用 Hash 缓存测试用例（仿照 [io.kudos.ability.cache.local.caffeine.keyvalue.NoCacheTest]）。
 * 缓存关闭时：MixHashCacheManager 未注入、getHashCache 抛异常、走 Hash 注解的方法每次执行且返回不同结果。
 *
 * @author K
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
        assertThrows<UninitializedPropertyAccessException> { mixHashCacheManager }
        assertThrows<IllegalStateException> { HashCacheKit.getHashCache("testHash") }

        val key = "key"
        val value1 = hashCacheableTestService.getFromDB(key)
        val value2 = hashCacheableTestService.getFromDB(key)
        assertNotEquals(value1.name, value2.name)
    }
}
