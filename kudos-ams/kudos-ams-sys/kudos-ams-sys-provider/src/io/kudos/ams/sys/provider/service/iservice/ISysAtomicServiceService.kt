package io.kudos.ams.sys.provider.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.provider.model.po.SysAtomicService
import io.kudos.ams.sys.common.vo.atomicservice.SysAtomicServiceCacheItem


/**
 * 原子服务业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysAtomicServiceService : IBaseCrudService<String, SysAtomicService> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据编码从缓存获取原子服务信息
     *
     * @param code 原子服务编码
     * @return SysAtomicServiceCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getAtomicServiceByCode(code: String): SysAtomicServiceCacheItem?

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 原子服务编码
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(code: String, active: Boolean): Boolean

    //endregion your codes 2

}
