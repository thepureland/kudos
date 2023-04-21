package io.kudos.ability.cache.local.caffeine

import io.kudos.test.common.TestSpringBootContextLoader
import org.soul.ability.cache.common.enums.CacheStrategy


/**
 * 本地缓存测试上下文加载器，用来动态修改属性
 *
 * @author K
 * @since 1.0.0
 */
class LocalCacheTestContextLoader : TestSpringBootContextLoader() {

    override fun getDynamicProperties(): Map<String, String> {
        return mapOf(
            "kudos.ability.cache.enabled" to "true",
            "cache.config.strategy" to CacheStrategy.SINGLE_LOCAL.name
        )
    }

}