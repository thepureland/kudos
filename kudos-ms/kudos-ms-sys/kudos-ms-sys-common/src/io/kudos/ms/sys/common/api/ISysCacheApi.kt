package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.cache.SysCacheCacheEntry
import io.kudos.ms.sys.common.vo.cache.SysCacheRow


/**
 * 缓存 对外API
 *
 * @author K
 * @since 1.0.0
 */
interface ISysCacheApi {


    /**
     * 加载指定名称的缓存，并缓存结果
     *
     * @param name 缓存名称
     * @return 缓存详情对象，找不到返回null
     */
    fun getCacheFromCache(name: String): SysCacheCacheEntry?

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 获取原子服务的缓存配置列表
     *
     * @param atomicServiceCode 原子服务编码
     * @return 缓存记录列表
     */
    fun getCachesByAtomicServiceCode(atomicServiceCode: String): List<SysCacheRow>

    /**
     * 获取所有启用的缓存配置
     *
     * @return 缓存记录列表
     */
    fun getAllActiveCaches(): List<SysCacheRow>


}