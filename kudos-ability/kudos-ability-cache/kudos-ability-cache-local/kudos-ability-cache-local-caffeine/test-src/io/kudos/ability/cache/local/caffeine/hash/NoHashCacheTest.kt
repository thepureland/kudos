package io.kudos.ability.cache.local.caffeine.hash

import io.kudos.ability.cache.common.kit.HashCacheKit
import io.kudos.test.common.init.EnableKudosTest
import org.springframework.context.annotation.Import
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import kotlin.test.Test
import kotlin.test.assertNull

/**
 * 缓存未启用时，[HashCacheKit.getHashCache] 应返回 null。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@EnableKudosTest
@Import(EmptyConfig::class)
internal class NoHashCacheTest {

    companion object {
        @DynamicPropertySource
        @JvmStatic
        private fun registerProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.cache.enabled") { "false" }
        }
    }

    @Test
    fun getHashCacheReturnsNullWhenCacheDisabled() {
        val cache = HashCacheKit.getHashCache("testHash")
        assertNull(cache)
    }
}

/** 空配置占位，避免测试上下文需要其他 Bean */
internal class EmptyConfig
