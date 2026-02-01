package io.kudos.ams.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.core.model.po.SysPortal
import io.kudos.ams.sys.common.vo.portal.SysPortalCacheItem
import io.kudos.ams.sys.common.vo.portal.SysPortalRecord
import io.kudos.ams.sys.common.vo.subsystem.SysSubSystemRecord


/**
 * 门户业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysPortalService : IBaseCrudService<String, SysPortal> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据编码从缓存获取门户信息
     *
     * @param code 门户编码
     * @return SysPortalCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getPortalByCode(code: String): SysPortalCacheItem?

    /**
     * 获取所有启用的门户
     *
     * @return 门户记录列表
     * @author K
     * @since 1.0.0
     */
    fun getAllActivePortals(): List<SysPortalRecord>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param code 门户编码
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(code: String, active: Boolean): Boolean

    /**
     * 获取门户下的子系统列表
     *
     * @param portalCode 门户编码
     * @return 子系统记录列表
     * @author K
     * @since 1.0.0
     */
    fun getSubSystemsByPortalCode(portalCode: String): List<SysSubSystemRecord>

    //endregion your codes 2

}