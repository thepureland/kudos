package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheEntry
import io.kudos.ms.sys.core.model.po.SysCache


/**
 * 缓存业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysCacheService : IBaseCrudService<String, SysCache> {

    /**
     * 按缓存配置主键 id 加载缓存配置，并缓存结果
     *
     * @param id 缓存配置主键，非空
     * @return 缓存详情对象，找不到返回 null
     */
    fun getCacheFromCache(id: String): SysCacheCacheEntry?

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 从缓存中获取原子服务的缓存配置列表
     *
     * @param atomicServiceCode 原子服务编码
     * @return 缓存记录列表
     */
    fun getCachesFromCache(atomicServiceCode: String): List<SysCacheCacheEntry>

    /**
     * 重载指定缓存配置（按 id）下某 key 的缓存项
     *
     * @param id 缓存配置主键，非空
     * @param key 缓存 key
     */
    fun reload(id: String, key: String)

    /**
     * 重载指定缓存配置（按 id）下的所有缓存项
     *
     * @param id 缓存配置主键，非空
     */
    fun reloadAll(id: String)

    /**
     * 踢除指定缓存配置（按 id）下某 key 的缓存项
     *
     * @param id 缓存配置主键，非空
     * @param key 缓存 key
     */
    fun evict(id: String, key: String)

    /**
     * 踢除指定缓存配置（按 id）下的所有缓存项
     *
     * @param id 缓存配置主键，非空
     */
    fun evictAll(id: String)

    /**
     * 检测指定缓存配置（按 id）下某 key 是否存在
     *
     * @param id 缓存配置主键，非空
     * @param key 缓存 key
     */
    fun existsKey(id: String, key: String): Boolean

    /**
     * 获取指定缓存配置（按 id）下某 key 的值的 json 表示
     *
     * @param id 缓存配置主键，非空
     * @param key 缓存 key
     * @return value 的 json 串，value 为 null 或出错返回空串
     */
    fun getValueJson(id: String, key: String): String


}