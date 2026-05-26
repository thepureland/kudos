package io.kudos.ability.cache.common.init.properties

import io.kudos.context.support.Consts
import org.springframework.beans.factory.annotation.Value

/**
 * Cache version and naming-isolation configuration.
 *
 * Prepends a `<version>:` prefix to the real key of every cache entry, providing isolation for
 * "canary release", "blue/green deployment", and "data format change" — after upgrading, simply
 * change `kudos.ability.cache.version` to coexist with old data without clashing.
 * The same prefix is also used as the channel prefix for distributed invalidation broadcasts ([realMsgChannel]).
 *
 * @author K
 * @since 1.0.0
 */
class CacheVersionConfig {

    /** Current cache version; empty string disables prefixing, equivalent to "no version isolation". */
    @Value($$"${kudos.ability.cache.version:default}")
    var cacheVersion: String = "default"

    /** Converts a logical cache name to its real, version-prefixed form (`<version>:<name>`). Returns the input unchanged when the version is empty. */
    fun getFinalCacheName(cacheName: String): String {
        if (cacheVersion.isBlank()) {
            return cacheName
        }
        return cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER + cacheName
    }

    /** Strips the version prefix back to the logical name; returns the input unchanged when the prefix does not match (back-compat with legacy unprefixed data). */
    fun getRealCacheName(cacheName: String): String {
        return if (cacheName.startsWith(cacheVersion)) {
            cacheName.replace(cacheVersion + Consts.CACHE_KEY_DEFAULT_DELIMITER, "")
        } else {
            cacheName
        }
    }

    /** Real channel for distributed invalidation broadcasts: `<version>:cache:local-remote:channel`. */
    val realMsgChannel: String
        get() = "$cacheVersion:$MSG_CHANNEL"

    companion object {
        private const val MSG_CHANNEL = "cache:local-remote:channel"
    }

}
