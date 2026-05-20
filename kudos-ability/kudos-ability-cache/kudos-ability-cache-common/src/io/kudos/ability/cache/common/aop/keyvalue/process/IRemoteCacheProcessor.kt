package io.kudos.ability.cache.common.aop.keyvalue.process

/**
 * 远端缓存底层处理器：[TenantAdvancedCacheable] / [TenantAdvancedCacheEvict] 的切面通过本接口
 * 直连远程（典型实现走 Redis），绕过 Spring 本地 CacheManager。
 *
 * 三个方法对应 get / put / evict 标准操作；`cacheKey + dataKey` 二级结构支持 Redis hash 形态
 * （cacheKey 作 hash key，dataKey 作 field name）。
 *
 * @author K
 * @since 1.0.0
 */
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
