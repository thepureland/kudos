package io.kudos.ability.cache.common.init.properties

import io.kudos.base.support.Consts
import org.springframework.beans.factory.annotation.Value

class CacheVersionConfig {

    @Value("\${soul.ability.cache.version:default}")
    var cacheVersion: String = "default"

    fun getFinalCacheName(cacheName: String): String {
        if (cacheVersion.isBlank()) {
            return cacheName
        }
        return cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER + cacheName
    }

    fun getRealCacheName(cacheName: String): String {
        if (cacheName.startsWith(cacheVersion)) {
            return cacheName.replace(cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER, "")
        } else {
            return cacheName
        }
    }

    val realMsgChannel: String
        get() = "$cacheVersion:$MSG_CHANNEL"

    companion object {
        private const val MSG_CHANNEL = "cache:local-remote:channel"
    }

}
