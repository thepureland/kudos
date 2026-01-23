package io.kudos.ams.sys.provider.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.provider.model.po.SysDomain
import io.kudos.ams.sys.common.vo.domain.SysDomainCacheItem
import io.kudos.ams.sys.common.vo.domain.SysDomainRecord


/**
 * 域名业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDomainService : IBaseCrudService<String, SysDomain> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据域名从缓存获取域名信息
     *
     * @param domain 域名
     * @return SysDomainCacheItem，找不到返回null
     * @author K
     * @since 1.0.0
     */
    fun getDomainByName(domain: String): SysDomainCacheItem?

    /**
     * 获取租户的域名列表
     *
     * @param tenantId 租户id
     * @return 域名记录列表
     * @author K
     * @since 1.0.0
     */
    fun getDomainsByTenantId(tenantId: String): List<SysDomainRecord>

    /**
     * 获取子系统的域名列表
     *
     * @param subSystemCode 子系统编码
     * @return 域名记录列表
     * @author K
     * @since 1.0.0
     */
    fun getDomainsBySubSystemCode(subSystemCode: String): List<SysDomainRecord>

    /**
     * 获取门户的域名列表
     *
     * @param portalCode 门户编码
     * @return 域名记录列表
     * @author K
     * @since 1.0.0
     */
    fun getDomainsByPortalCode(portalCode: String): List<SysDomainRecord>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 域名id
     * @param active 是否启用
     * @return 是否更新成功
     * @author K
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    //endregion your codes 2

}