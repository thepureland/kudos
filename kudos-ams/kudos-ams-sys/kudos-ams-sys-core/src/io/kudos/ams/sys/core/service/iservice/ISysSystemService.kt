package io.kudos.ams.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.core.model.po.SysSystem
import io.kudos.ams.sys.common.vo.system.SysSystemCacheItem
import io.kudos.ams.sys.common.vo.system.SysSystemRecord
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemRecord


/**
 * 系统业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysSystemService : IBaseCrudService<String, SysSystem> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据编码从缓存获取系统信息
     *
     * @param code 系统编码
     * @return SysSystemCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getSystemByCode(code: String): SysSystemCacheItem?

    /**
     * 获取所有启用的系统
     *
     * @return 系统记录列表
     * @author K
     * @since 1.0.0
     */
    fun getAllActiveSystems(): List<SysSystemRecord>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 系统编码
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(code: String, active: Boolean): Boolean

    /**
     * 获取系统下的子系统列表
     *
     * @param systemCode 系统编码
     * @return 子系统记录列表
     * @author K
     * @since 1.0.0
     */
    fun getSubSystemsBySystemCode(systemCode: String): List<SysSubSystemRecord>

    //endregion your codes 2

}