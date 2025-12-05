package io.kudos.ams.sys.provider.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ams.sys.provider.model.po.SysTenantSubSystem
import io.kudos.ams.sys.provider.model.table.SysTenantSubSystems
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 租户-子系统关系数据访问对象
 *
 * @author K
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantSubSystemDao : BaseCrudDao<String, SysTenantSubSystem, SysTenantSubSystems>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id查找对应的子系统编码
     *
     * @param tenantId 租户id
     * @return Set<子系统编码>
     */
    fun searchSubSystemCodesByTenantId(tenantId: String): Set<String> {
        val criteria = Criteria.of(SysTenantSubSystem::tenantId.name, OperatorEnum.EQ, tenantId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantSubSystem::subSystemCode.name).toSet() as Set<String>
    }

    /**
     * 根据子系统编码查找对应的租户id
     *
     * @param subSystemCode 子系统编码
     * @return Set<租户id>
     */
    fun searchTenantIdsBySubSystemCode(subSystemCode: String): Set<String> {
        val criteria = Criteria.of(SysTenantSubSystem::subSystemCode.name, OperatorEnum.EQ, subSystemCode)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantSubSystem::tenantId.name).toSet() as Set<String>
    }

    /**
     * 根据租户id对子系统编码进行分组
     *
     * @param tenantIds 查询条件：租户id集合，为null时将查出所有记录，默认为null
     * @return Map<租户id， List<子系统编码>>
     */
    fun groupingSubSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>> {
        val returnProperties = listOf(SysTenantSubSystem::tenantId.name, SysTenantSubSystem::subSystemCode.name)
        @Suppress("UNCHECKED_CAST")
        val results = if (tenantIds == null) {
            allSearchProperties(returnProperties)
        } else {
            val criteria = Criteria.of(SysTenantSubSystem::tenantId.name, OperatorEnum.IN, tenantIds)
            searchProperties(criteria, returnProperties)
        } as List<Map<String, String>>
        return results.groupBy(
            keySelector = { it[SysTenantSubSystem::tenantId.name]!! },
            valueTransform = { it[SysTenantSubSystem::subSystemCode.name]!! }
        )
    }

    /**
     * 根据子系统编码对租户id进行分组
     *
     * @param subSystemCodes 查询条件：子系统编码集合，为null时将查出所有记录，默认为null
     * @return Map<子系统编码， List<租户id>>
     */
    fun groupingTenantIdsBySubSystemCodes(subSystemCodes: Collection<String>? = null): Map<String, List<String>> {
        val returnProperties = listOf(SysTenantSubSystem::subSystemCode.name, SysTenantSubSystem::tenantId.name)
        @Suppress("UNCHECKED_CAST")
        val results = if (subSystemCodes == null) {
            allSearchProperties(returnProperties)
        } else {
            val criteria = Criteria.of(SysTenantSubSystem::subSystemCode.name, OperatorEnum.IN, subSystemCodes)
            searchProperties(criteria, returnProperties)
        } as List<Map<String, String>>
        return results.groupBy(
            keySelector = { it[SysTenantSubSystem::subSystemCode.name]!! },
            valueTransform = { it[SysTenantSubSystem::tenantId.name]!! }
        )
    }

    //endregion your codes 2

}