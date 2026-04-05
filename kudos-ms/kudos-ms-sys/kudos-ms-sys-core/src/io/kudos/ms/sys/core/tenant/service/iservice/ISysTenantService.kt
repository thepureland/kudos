package io.kudos.ms.sys.core.tenant.service.iservice
import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.sys.common.tenant.vo.SysTenantCacheEntry
import io.kudos.ms.sys.common.tenant.vo.response.SysTenantRow
import io.kudos.ms.sys.core.tenant.model.po.SysTenant


/**
 * 租户业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface ISysTenantService : IBaseCrudService<String, SysTenant> {

    /**
     * 按租户 id 加载租户缓存项，并缓存结果（含未启用）
     */
    fun getTenantFromCache(id: String): SysTenantCacheEntry?

    /**
     * 按 id 集合批量加载租户缓存项（含未启用）
     */
    fun getTenantsFromCacheByIds(ids: Collection<String>): Map<String, SysTenantCacheEntry>

    /**
     * 按子系统编码从缓存解析租户列表（含未启用；绑定关系来自租户-系统 Hash 缓存）
     */
    fun getTenantsForSubSystemFromCache(subSystemCode: String): List<SysTenantCacheEntry>

    /**
     * 从数据库加载全部租户为缓存载体类型（与库里一致；用于需全量列表场景，非 Spring Cache 全量扫描）
     */
    fun getAllTenantsFromCache(): List<SysTenantCacheEntry>

    /**
     * 更新启用状态，并同步缓存
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * 根据 id 获取租户列表行（直查库，非缓存）
     */
    fun getTenantRecord(id: String): SysTenantRow?

    /**
     * 根据名称获取租户记录
     */
    fun getTenantByName(name: String): SysTenantRow?

    /**
     * 从缓存获取租户已绑定的子系统编码集合
     */
    fun getSubSystemCodesFromCache(tenantId: String): Set<String>


}
