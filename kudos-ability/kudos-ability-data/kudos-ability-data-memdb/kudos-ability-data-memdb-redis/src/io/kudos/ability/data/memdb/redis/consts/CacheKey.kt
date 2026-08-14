package io.kudos.ability.data.memdb.redis.consts


/**
 * Cache key assembly utility. Unifies the segmentation scheme for Redis keys:
 *  - Business sub-segments (namespace, table name, property, value) are separated by [CACHE_KEY_SEPARATOR] (`:`).
 *  - The "prefix" of composite primary keys / multi-id structures uses [CACHE_KEY_PREFIX_SEPARATOR] (`,`).
 *
 * `:` is chosen by Redis ecosystem convention; `,` is chosen to distinguish from `:` / `-` that may appear in business ids.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object CacheKey {

    /** Separator between sub-keys; business-meaningful, used to split the key back into its original segments. */
    const val CACHE_KEY_SEPARATOR: String = ":"

    /** Separator for cache key prefixes, such as the `,` in a session key like `1,1,1:subkey`. */
    const val CACHE_KEY_PREFIX_SEPARATOR: String = ","

    /** Misspelled legacy alias of [CACHE_KEY_SEPARATOR]; kept for source compatibility. */
    @Deprecated("Misspelled; use CACHE_KEY_SEPARATOR", ReplaceWith("CacheKey.CACHE_KEY_SEPARATOR"))
    const val CACHE_KEY_SEPERATOR: String = CACHE_KEY_SEPARATOR

    /** Misspelled legacy alias of [CACHE_KEY_PREFIX_SEPARATOR]; kept for source compatibility. */
    @Deprecated("Misspelled; use CACHE_KEY_PREFIX_SEPARATOR", ReplaceWith("CacheKey.CACHE_KEY_PREFIX_SEPARATOR"))
    const val CACHE_KEY_PREFIX_SEPERATOR: String = CACHE_KEY_PREFIX_SEPARATOR

    /**
     * Composes a cache key by joining the given parameters with the separator.
     *
     * @param keys keys to be combined
     * @return Cache key composed of the parameters joined by the separator.
     */
    fun getCacheKey(vararg keys: String): String {
        return keys.joinToString(CACHE_KEY_SEPARATOR)
    }

    /**
     * Cache prefix key composition, used for sessions; the parameters are joined by the connecting character.
     *
     * @param keys keys to be combined
     * @return Cache key composed of the parameters joined by the separator.
     */
    fun getCacheKeyPrefix(vararg keys: String): String {
        return keys.joinToString(CACHE_KEY_PREFIX_SEPARATOR)
    }

}
