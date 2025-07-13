package io.kudos.ams.sys.service.biz.ibiz

import io.kudos.base.support.biz.IBaseCrudBiz
import io.kudos.ams.sys.service.model.po.SysTenantSubSystem


/**
 * 租户-子系统关系业务接口
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface ISysTenantSubSystemBiz : IBaseCrudBiz<String, SysTenantSubSystem> {
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

    //endregion your codes 2

}