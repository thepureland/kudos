package io.kudos.ams.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.core.model.po.SysSubSystem
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemCacheItem
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemRecord
import io.kudos.ams.sys.common.vo.microservice.SysMicroServiceRecord


/**
 * 子系统业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysSubSystemService : IBaseCrudService<String, SysSubSystem> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据编码从缓存获取子系统信息
     *
     * @param code 子系统编码
     * @return SysSubSystemCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getSubSystemByCode(code: String): SysSubSystemCacheItem?

    /**
     * 按门户编码查询子系统列表
     *
     * @param portalCode 门户编码
     * @return 子系统记录列表
     * @author K
     * @since 1.0.0
     */
    fun getSubSystemsByPortalCode(portalCode: String): List<SysSubSystemRecord>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 子系统编码
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(code: String, active: Boolean): Boolean

    /**
     * 获取子系统下的微服务列表
     *
     * @param subSystemCode 子系统编码
     * @return 微服务记录列表
     * @author K
     * @since 1.0.0
     */
    fun getMicroServicesBySubSystemCode(subSystemCode: String): List<SysMicroServiceRecord>

    //endregion your codes 2

}