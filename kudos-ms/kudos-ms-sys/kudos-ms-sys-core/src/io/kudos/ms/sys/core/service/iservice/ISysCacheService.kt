package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.common.vo.cache.SysCacheCacheItem
import io.kudos.ms.sys.common.vo.cache.SysCacheRecord
import io.kudos.ms.sys.core.model.po.SysCache


/**
 * 缓存业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface ISysCacheService : IBaseCrudService<String, SysCache> {
//endregion your codes 1

    //region your codes 2

    /**
     * 加载指定名称的缓存，并缓存结果
     *
     * @param name 缓存名称
     * @return 缓存详情对象，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getCacheFromCache(name: String): SysCacheCacheItem?

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 获取原子服务的缓存配置列表
     *
     * @param atomicServiceCode 原子服务编码
     * @return 缓存记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getCachesByAtomicServiceCode(atomicServiceCode: String): List<SysCacheRecord>

    /**
     * 获取所有启用的缓存配置
     *
     * @return 缓存记录列表
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getAllActiveCaches(): List<SysCacheRecord>

    /**
     * 重载指定缓存名称和key的缓存项
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    fun reload(name: String, key: String)

    /**
     * 重载指定缓存名称的所有缓存项
     *
     * @param name 缓存名称
     */
    fun reloadAll(name: String)

    /**
     * 踢除指定缓存名称和key的缓存项
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    fun evict(name: String, key: String)

    /**
     * 踢除指定缓存名称的所有缓存项
     *
     * @param name 缓存名称
     */
    fun evictAll(name: String)

    /**
     * 检测指定名称和key的缓存项是否存在
     *
     * @param name 缓存名称
     * @param key 缓存key
     */
    fun existsKey(name: String, key: String): Boolean

    /**
     * 获取指定名称和key的缓存项的值的json表示
     *
     * @param name 缓存名称
     * @param key 缓存key
     * @return value的json串,value为null或出错返回空串
     */
    fun getValueJson(name: String, key: String): String

    //endregion your codes 2

}