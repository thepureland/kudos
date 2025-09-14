package io.kudos.ability.data.memdb.redis.consts


object CacheKey {

    /*子key的分隔符,分隔符之间有业务意义,会被根据些分隔符,进行折KEY*/
    const val CACHE_KEY_SEPERATOR: String = ":"

    /*缓存键前缀的分隔符号 key like: 1,1,1:subkey*/
    const val CACHE_KEY_PREFIX_SEPERATOR: String = ","

    /*缓存2.0后缀*/
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
