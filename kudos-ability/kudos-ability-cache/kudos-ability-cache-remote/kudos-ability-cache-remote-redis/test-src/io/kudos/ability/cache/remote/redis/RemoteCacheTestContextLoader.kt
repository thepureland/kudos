package io.kudos.ability.cache.remote.redis

import io.kudos.test.common.TestSpringBootContextLoader
import org.soul.ability.cache.common.enums.CacheStrategy

class RemoteCacheTestContextLoader : TestSpringBootContextLoader() {

    override fun getDynamicProperties(): Map<String, String> {
        return mapOf(
            "kudos.ability.cache.enabled" to "true",
            "cache.config.strategy" to CacheStrategy.REMOTE.name
        )
    }

}