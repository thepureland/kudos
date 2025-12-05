package io.kudos.ams.sys.provider.service.iservice

import io.kudos.ams.sys.common.vo.datasource.SysDataSourceCacheItem
import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.provider.model.po.SysDataSource


/**
 * 数据源业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDataSourceService : IBaseCrudService<String, SysDataSource> {
//endregion your codes 1

    //region your codes 2

    /**
     * 返回指定租户、子系统、微服务、原子服务的数据源，先从缓存找，找不到从数据库加载，并缓存
     *
     * @param tenantId 租户id
     * @param subSystemCode 子系统编码
     * @param microServiceCode 微服务编码
     * @param atomicServiceCode 原子服务编码
     * @return SysDataSourceCacheItem
     * @author K
     * @since 1.0.0
     */
    fun getDataSource(
        tenantId: String,
        subSystemCode: String,
        microServiceCode: String?,
        atomicServiceCode: String?
    ): SysDataSourceCacheItem?

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
     * 重置密码
     *
     * @param id 主键
     * @param newPassword 新密码
     * @author K
     * @since 1.0.0
     */
    fun resetPassword(id: String, newPassword: String)

    //endregion your codes 2

}