package io.kudos.ams.sys.core.service.iservice

import io.kudos.base.support.iservice.IBaseCrudService
import io.kudos.ams.sys.core.model.po.SysTenantSubSystem


/**
 * 租户-子系统关系业务接口
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantSubSystemService : IBaseCrudService<String, SysTenantSubSystem> {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id查找对应的子系统编码
     *
     * @param tenantId 租户id
     * @return Set<子系统编码>
     */
    fun searchSubSystemCodesByTenantId(tenantId: String): Set<String>

    /**
     * 根据子系统编码查找对应的租户id
     *
     * @param subSystemCode 子系统编码
     * @return Set<租户id>
     */
    fun searchTenantIdsBySubSystemCode(subSystemCode: String): Set<String>

    /**
     * 根据租户id对子系统编码进行分组
     *
     * @param tenantIds 查询条件：租户id集合，为null时将查出所有记录，默认为null
     * @return Map<租户id， List<子系统编码>>
     */
    fun groupingSubSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>>

    /**
     * 根据子系统编码对租户id进行分组
     *
     * @param subSystemCodes 查询条件：子系统编码集合，为null时将查出所有记录，默认为null
     * @return Map<子系统编码， List<租户id>>
     */
    fun groupingTenantIdsBySubSystemCodes(subSystemCodes: Collection<String>? = null): Map<String, List<String>>

    /**
     * 批量绑定租户与子系统的关系
     *
     * @param tenantId 租户id
     * @param subSystemCodes 子系统编码集合
     * @param portalCode 门户编码
     * @return 成功绑定的数量
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun batchBind(tenantId: String, subSystemCodes: Collection<String>, portalCode: String): Int

    /**
     * 解绑租户与子系统的关系
     *
     * @param tenantId 租户id
     * @param subSystemCode 子系统编码
     * @return 是否解绑成功
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun unbind(tenantId: String, subSystemCode: String): Boolean

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param subSystemCode 子系统编码
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(tenantId: String, subSystemCode: String): Boolean

    //endregion your codes 2

}