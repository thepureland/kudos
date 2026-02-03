package io.kudos.ms.sys.common.api

import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceRecord


/**
 * 微服务 对外API
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysMicroServiceApi {
//endregion your codes 1

    //region your codes 2

    /**
     * 返回所有启用的微服务(包括原子服务，原子服务本质上也是微服务)
     *
     * @return List<微服务(包括原子服务)缓存对象>
     * @author K
     * @since 1.0.0
     */
    fun getAllActiveMicroService(): List<SysMicroServiceCacheItem>

    /**
     * 返回所有启用的微服务,不包括原子服务
     *
     * @return List<微服务(不包括原子服务)缓存对象>，不存在返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getAllActiveMicroServiceExcludeAtomicService(): List<SysMicroServiceCacheItem>

    /**
     * 返回所有启用的原子服务(atomicService为true的微服务)
     *
     * @return List<原子服务(微服务)缓存对象>, 不存在返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getAllActiveAtomicService(): List<SysMicroServiceCacheItem>

    /**
     * 返回指定编码的微/原子服务，可能是未启用的
     *
     * @param code 微/原子服务编码
     * @return 微/原子服务缓存对象，不存在返回null
     * @author K
     * @since 1.0.0
     */
    fun getMicroServiceByCode(code: String): SysMicroServiceCacheItem?

    /**
     * 返回指定父编码下的所有启用的原子服务
     *
     * @param parentCode 父编码（微服务编码）
     * @return List<SysMicroServiceRecord>, 不存在返回空列表
     * @author K
     * @since 1.0.0
     */
    fun getAllActiveAtomicServiceByParentCode(parentCode: String): List<SysMicroServiceRecord>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 微服务编码
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(code: String, active: Boolean): Boolean

    //endregion your codes 2

}