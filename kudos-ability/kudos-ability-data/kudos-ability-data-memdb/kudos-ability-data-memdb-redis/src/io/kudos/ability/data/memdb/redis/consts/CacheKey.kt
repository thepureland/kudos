package io.kudos.ability.data.memdb.redis.consts


/**
 * Cache key assembly utility. Unifies the segmentation scheme for Redis keys:
 *  - Business sub-segments (namespace, table name, property, value) are separated by [CACHE_KEY_SEPERATOR] (`:`).
 *  - The "prefix" of composite primary keys / multi-id structures uses [CACHE_KEY_PREFIX_SEPERATOR] (`,`).
 *
 * `:` is chosen by Redis ecosystem convention; `,` is chosen to distinguish from `:` / `-` that may appear in business ids.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object CacheKey {

    /** Separator between sub-keys; business-meaningful, used to split the key back into its original segments. */
    const val CACHE_KEY_SEPERATOR: String = ":"

    /** Separator for cache key prefixes, such as the `,` in a session key like `1,1,1:subkey`. */
    const val CACHE_KEY_PREFIX_SEPERATOR: String = ","

    /**
     * Composes a cache key by joining the given parameters with the separator.
     *
     * @param keys keys to be combined
     * @return Cache key composed of the parameters joined by the separator.
     */
    fun getCacheKey(vararg keys: String): String {
        return keys.joinToString(CACHE_KEY_SEPERATOR)
    }

    /**
     * Cache prefix key composition, used for sessions; the parameters are joined by the connecting character.
     *
     * @param keys keys to be combined
     * @return Cache key composed of the parameters joined by the separator.
     */
    fun getCacheKeyPrefix(vararg keys: String): String {
        return keys.joinToString(CACHE_KEY_PREFIX_SEPERATOR)
    }

}
