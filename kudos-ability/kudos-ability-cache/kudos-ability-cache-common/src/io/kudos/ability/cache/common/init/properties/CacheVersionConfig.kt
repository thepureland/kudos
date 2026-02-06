package io.kudos.ability.cache.common.init.properties

import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Value

/**
 * 缓存版本配置类
 * 用于管理缓存版本，支持缓存版本隔离和缓存名称转换
 */
class CacheVersionConfig {

    @Value($$"${kudos.ability.cache.version:default}")
    var cacheVersion: String = "default"

    fun getFinalCacheName(cacheName: String): String {
        if (cacheVersion.isBlank()) {
            return cacheName
        }
        return cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER + cacheName
    }

    fun getRealCacheName(cacheName: String): String {
        return if (cacheName.startsWith(cacheVersion)) {
            cacheName.replace(cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER, "")
        } else {
            cacheName
        }
    }

    val realMsgChannel: String
        get() = "$cacheVersion:$MSG_CHANNEL"

    companion object {
        private const val MSG_CHANNEL = "cache:local-remote:channel"
    }

}
