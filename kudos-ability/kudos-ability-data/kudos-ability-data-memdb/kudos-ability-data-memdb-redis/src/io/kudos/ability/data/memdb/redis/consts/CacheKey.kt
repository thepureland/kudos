package io.kudos.ability.data.memdb.redis.consts


/**
 * 缓存 key 拼装小工具。统一 Redis key 的分段方式：
 *  - 业务子段（namespace、表名、属性、值）之间用 [CACHE_KEY_SEPERATOR] (`:`)
 *  - 组合主键 / 多 id 一类的"前缀"内部用 [CACHE_KEY_PREFIX_SEPERATOR] (`,`)
 *
 * 选 `:` 是 Redis 生态约定；选 `,` 是为了和业务 id 中可能出现的 `:` / `-` 区分开。
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object CacheKey {

    /** 子 key 之间的分隔符，业务有意义，会按此切分 key 还原原始段。 */
    const val CACHE_KEY_SEPERATOR: String = ":"

    /** 缓存键前缀的分隔符，如 session key `1,1,1:subkey` 中的 `,`。 */
    const val CACHE_KEY_PREFIX_SEPERATOR: String = ","

    /**
     * 缓存Key组成方式,由各参数加分隔符组成
     *
     * @param keys 需要组合的keys
     * @return 参数加分隔符组成缓存Key
     */
    fun getCacheKey(vararg keys: String): String {
        return keys.joinToString(CACHE_KEY_SEPERATOR)
    }

    /**
     * 缓存前缀Key组合，用于session，各参数由链接字符组合
     *
     * @param keys 需要组合的keys
     * @return 参数加分隔符组成缓存Key
     */
    fun getCacheKeyPrefix(vararg keys: String): String {
        return keys.joinToString(CACHE_KEY_PREFIX_SEPERATOR)
    }

}
