package io.kudos.ms.sys.core.domain.service.iservice
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.domain.vo.SysDomainCacheEntry
import io.kudos.ms.sys.common.domain.vo.response.SysDomainRow
import io.kudos.ms.sys.core.domain.model.po.SysDomain


/**
 * 域名业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysDomainService : IBaseCrudService<String, SysDomain> {

    /**
     * 按域名名称加载域名缓存项（仅 active=true 会写入/命中按名称缓存）
     *
     * @param domainName 域名名称，非空
     * @return 缓存项；找不到或未启用返回 null
     */
    fun getDomainFromCache(domainName: String): SysDomainCacheEntry?

    /**
     * 获取租户的域名列表（直查库）
     *
     * @param tenantId 租户 id
     */
    fun getDomainsByTenantId(tenantId: String): List<SysDomainRow>

    /**
     * 获取系统的域名列表（直查库）
     *
     * @param systemCode 系统编码
     */
    fun getDomainsBySystemCode(systemCode: String): List<SysDomainRow>

    /**
     * 更新启用状态，并同步缓存
     *
     * @param id 域名 id
     * @param active 是否启用
     * @return 是否更新成功
     */
    fun updateActive(id: String, active: Boolean): Boolean

}
