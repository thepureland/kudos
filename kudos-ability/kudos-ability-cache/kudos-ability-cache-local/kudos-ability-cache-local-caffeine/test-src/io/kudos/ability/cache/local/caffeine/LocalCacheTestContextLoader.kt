package io.kudos.ability.cache.local.caffeine

import io.kudos.test.common.TestSpringBootContextLoader
import org.soul.ability.cache.common.enums.CacheStrategy

class LocalCacheTestContextLoader : TestSpringBootContextLoader() {

    override fun getDynamicProperties(): Map<String, String> {
        return mapOf(
            "kudos.ability.cache.enabled" to "true",
            "cache.config.strategy" to CacheStrategy.SINGLE_LOCAL.name
        )
    }

}