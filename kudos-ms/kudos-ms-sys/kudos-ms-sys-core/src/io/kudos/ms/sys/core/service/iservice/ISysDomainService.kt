package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.common.api.ISysDomainApi
import io.kudos.ms.sys.common.vo.domain.SysDomainCacheItem
import io.kudos.ms.sys.common.vo.domain.SysDomainRecord
import io.kudos.ms.sys.core.model.po.SysDomain


/**
 * 域名业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysDomainService : IBaseCrudService<String, SysDomain>, ISysDomainApi {
//endregion your codes 1

    //region your codes 2

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
     * 获取系统的域名列表
     *
     * @param systemCode 系统编码
     * @return 域名记录列表
     * @author K
     * @since 1.0.0
     */
    fun getDomainsBySystemCode(systemCode: String): List<SysDomainRecord>

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
