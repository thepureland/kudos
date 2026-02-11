package io.kudos.ms.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ms.sys.core.model.po.SysTenantSystem


/**
 * 租户-系统关系业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantSystemService : IBaseCrudService<String, SysTenantSystem> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id查找对应的系统编码
     *
     * @param tenantId 租户id
     * @return Set<系统编码>
     */
    fun searchSystemCodesByTenantId(tenantId: String): Set<String>

    /**
     * 根据系统编码查找对应的租户id
     *
     * @param systemCode 系统编码
     * @return Set<租户id>
     */
    fun searchTenantIdsBySystemCode(systemCode: String): Set<String>

    /**
     * 根据租户id对系统编码进行分组
     *
     * @param tenantIds 查询条件：租户id集合，为null时将查出所有记录，默认为null
     * @return Map<租户id， List<系统编码>>
     */
    fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>>

    /**
     * 根据系统编码对租户id进行分组
     *
     * @param systemCodes 查询条件：系统编码集合，为null时将查出所有记录，默认为null
     * @return Map<系统编码， List<租户id>>
     */
    fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>? = null): Map<String, List<String>>

    /**
     * 批量绑定租户与系统的关系
     *
     * @param tenantId 租户id
     * @param systemCodes 系统编码集合
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, systemCodes: Collection<String>): Int

    /**
     * 解绑租户与系统的关系
     *
     * @param tenantId 租户id
     * @param systemCode 系统编码
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(tenantId: String, systemCode: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param systemCode 系统编码
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(tenantId: String, systemCode: String): Boolean

    /**
     * 按租户ID删除其全部租户-系统关系
     *
     * @param tenantId 租户ID
     * @return 删除条数
     */
    fun deleteByTenantId(tenantId: String): Int

    /**
     * 按租户ID集合批量删除租户-系统关系
     *
     * @param tenantIds 租户ID集合
     * @return 删除条数
     */
    fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int

    //endregion your codes 2

}
