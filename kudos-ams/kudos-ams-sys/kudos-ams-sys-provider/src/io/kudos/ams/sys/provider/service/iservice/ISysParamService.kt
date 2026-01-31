package io.kudos.ams.sys.provider.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.provider.model.po.SysParam
import io.kudos.ams.sys.common.vo.param.SysParamCacheItem
import io.kudos.ams.sys.common.vo.param.SysParamRecord


/**
 * 参数业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysParamService : IBaseCrudService<String, SysParam> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据模块编码和参数名称从缓存获取参数信息
     *
     * @param atomicServiceCode 模块编码
     * @param paramName 参数名称
     * @return SysParamCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getParamByAtomicServiceAndName(atomicServiceCode: String, paramName: String): SysParamCacheItem?

    /**
     * 获取模块的所有参数
     *
     * @param atomicServiceCode 模块编码
     * @return 参数记录列表
     * @author K
     * @since 1.0.0
     */
    fun getParamsByAtomicServiceCode(atomicServiceCode: String): List<SysParamRecord>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 参数id
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 获取参数值，如果找不到则返回默认值
     *
     * @param atomicServiceCode 模块编码
     * @param paramName 参数名称
     * @param defaultValue 默认值，如果为null且参数不存在则返回null
     * @return 参数值
     * @author K
     * @since 1.0.0
     */
    fun getParamValue(atomicServiceCode: String, paramName: String, defaultValue: String? = null): String?

    //endregion your codes 2

}