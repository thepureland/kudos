package io.kudos.ability.cache.common.aop.process

interface IRemoteCacheProcessor {
    /**
     * 获取缓存数据
     * @param cacheKey
     * @param dataKey
     * @return
     */
    fun getCacheData(cacheKey: String, dataKey: String): Any?

    /**
     * 写入缓存数据
     * @param cacheKey
     * @param dataKey
     * @param o
     * @param timeOut
     */
    fun writeCacheData(cacheKey: String, dataKey: String, o: Any?, timeOut: Long)

    /**
     * 清理缓存
     * @param cacheKey
     * @param s
     * @param b
     */
    fun clearCache(cacheKey: String, s: String, b: Boolean)
}
