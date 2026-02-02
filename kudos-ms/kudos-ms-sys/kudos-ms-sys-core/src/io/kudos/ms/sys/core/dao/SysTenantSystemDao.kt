package io.kudos.ms.sys.core.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.sys.core.model.po.SysTenantSystem
import io.kudos.ms.sys.core.model.table.SysTenantSystems
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 租户-系统关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantSystemDao : BaseCrudDao<String, SysTenantSystem, SysTenantSystems>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id查找对应的系统编码
     *
     * @param tenantId 租户id
     * @return Set<系统编码>
     */
    fun searchSystemCodesByTenantId(tenantId: String): Set<String> {
        val criteria = Criteria.of(SysTenantSystem::tenantId.name, OperatorEnum.EQ, tenantId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantSystem::systemCode.name).toSet() as Set<String>
    }

    /**
     * 根据系统编码查找对应的租户id
     *
     * @param systemCode 系统编码
     * @return Set<租户id>
     */
    fun searchTenantIdsBySystemCode(systemCode: String): Set<String> {
        val criteria = Criteria.of(SysTenantSystem::systemCode.name, OperatorEnum.EQ, systemCode)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantSystem::tenantId.name).toSet() as Set<String>
    }

    /**
     * 根据租户id对系统编码进行分组
     *
     * @param tenantIds 查询条件：租户id集合，为null时将查出所有记录，默认为null
     * @return Map<租户id， List<系统编码>>
     */
    fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>> {
        val returnProperties = listOf(SysTenantSystem::tenantId.name, SysTenantSystem::systemCode.name)
        @Suppress("UNCHECKED_CAST")
        val results = if (tenantIds == null) {
            allSearchProperties(returnProperties)
        } else {
            val criteria = Criteria.of(SysTenantSystem::tenantId.name, OperatorEnum.IN, tenantIds)
            searchProperties(criteria, returnProperties)
        } as List<Map<String, String>>
        return results.groupBy(
            keySelector = { it[SysTenantSystem::tenantId.name]!! },
            valueTransform = { it[SysTenantSystem::systemCode.name]!! }
        )
    }

    /**
     * 根据系统编码对租户id进行分组
     *
     * @param systemCodes 查询条件：系统编码集合，为null时将查出所有记录，默认为null
     * @return Map<系统编码， List<租户id>>
     */
    fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>? = null): Map<String, List<String>> {
        val returnProperties = listOf(SysTenantSystem::systemCode.name, SysTenantSystem::tenantId.name)
        @Suppress("UNCHECKED_CAST")
        val results = if (systemCodes == null) {
            allSearchProperties(returnProperties)
        } else {
            val criteria = Criteria.of(SysTenantSystem::systemCode.name, OperatorEnum.IN, systemCodes)
            searchProperties(criteria, returnProperties)
        } as List<Map<String, String>>
        return results.groupBy(
            keySelector = { it[SysTenantSystem::systemCode.name]!! },
            valueTransform = { it[SysTenantSystem::tenantId.name]!! }
        )
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param systemCode 系统编码
     * @return 是否存在
     * @author AI: Cursor
     */
    fun exists(tenantId: String, systemCode: String): Boolean {
        val criteria = Criteria.of(SysTenantSystem::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(SysTenantSystem::systemCode.name, OperatorEnum.EQ, systemCode)
        return count(criteria) > 0
    }

    //endregion your codes 2

}
