package io.kudos.ams.sys.provider.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.provider.model.po.SysModule
import io.kudos.ams.sys.common.vo.module.SysModuleCacheItem
import io.kudos.ams.sys.common.vo.module.SysModuleRecord


/**
 * 模块业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysModuleService : IBaseCrudService<String, SysModule> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据编码从缓存获取模块信息
     *
     * @param code 模块编码
     * @return SysModuleCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getModuleByCode(code: String): SysModuleCacheItem?

    /**
     * 按原子服务编码查询模块列表
     *
     * @param atomicServiceCode 原子服务编码
     * @return 模块记录列表
     * @author K
     * @since 1.0.0
     */
    fun getModulesByAtomicServiceCode(atomicServiceCode: String): List<SysModuleRecord>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 模块编码
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(code: String, active: Boolean): Boolean

    //endregion your codes 2

}