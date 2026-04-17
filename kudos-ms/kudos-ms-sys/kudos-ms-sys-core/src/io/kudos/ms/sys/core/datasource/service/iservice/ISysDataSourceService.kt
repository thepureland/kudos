package io.kudos.ms.sys.core.datasource.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.datasource.vo.SysDataSourceCacheEntry
import io.kudos.ms.sys.common.datasource.vo.response.SysDataSourceRow
import io.kudos.ms.sys.core.datasource.model.po.SysDataSource


/**
 * 数据源业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDataSourceService : IBaseCrudService<String, SysDataSource> {

    /**
     * 按主键 id 加载数据源缓存项，并缓存结果
     *
     * @param id 数据源主键，非空
     * @return 缓存项，找不到返回 null
     */
    fun getDataSourceFromCache(id: String): SysDataSourceCacheEntry?

    /**
     * 按租户 id、子系统编码、微服务编码从缓存查询数据源列表（含未启用）
     */
    fun getDataSourcesFromCache(
        tenantId: String,
        subSystemCode: String,
        microServiceCode: String?
    ): List<SysDataSourceCacheEntry>

    /**
     * 按租户 id 与原子服务编码从缓存取一条数据源（内部按 tenantId + subSystem=null + microService=atomicServiceCode 查询后取首条）
     */
    fun getDataSourceFromCache(tenantId: String, atomicServiceCode: String?): SysDataSourceCacheEntry?

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 主键
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 重置密码
     *
     * @param id 主键
     * @param newPassword 新密码
     */
    fun resetPassword(id: String, newPassword: String)

    /**
     * 获取租户的数据源列表
     *
     * @param tenantId 租户id
     * @return 数据源记录列表
     */
    fun getDataSourcesByTenantId(tenantId: String): List<SysDataSourceRow>

    /**
     * 获取子系统的数据源列表
     *
     * @param subSystemCode 子系统编码
     * @return 数据源记录列表
     */
    fun getDataSourcesBySubSystemCode(subSystemCode: String): List<SysDataSourceRow>


}
