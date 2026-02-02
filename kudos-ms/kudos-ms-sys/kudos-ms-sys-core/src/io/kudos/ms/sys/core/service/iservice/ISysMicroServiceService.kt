package io.kudos.ms.sys.core.service.iservice

import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceCacheItem
import io.kudos.ms.sys.common.vo.microservice.SysMicroServiceRecord
import io.kudos.ms.sys.core.model.po.SysMicroService
import io.kudos.base.support.iservice.IBaseCrudService


/**
 * 微服务业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysMicroServiceService : IBaseCrudService<String, SysMicroService> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据编码从缓存获取微服务信息
     *
     * @param code 微服务编码
     * @return SysMicroServiceCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getMicroServiceByCode(code: String): SysMicroServiceCacheItem?

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

    /**
     * 获取微服务下的原子服务列表
     *
     * @param microServiceCode 微服务编码
     * @return 原子服务记录列表
     * @author K
     * @since 1.0.0
     */
    fun getAtomicServicesByMicroServiceCode(microServiceCode: String): List<SysMicroServiceRecord>

    //endregion your codes 2

}