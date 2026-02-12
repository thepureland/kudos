package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.sys.core.model.po.SysTenantSystem
import io.kudos.ms.sys.core.model.table.SysTenantSystems
import org.springframework.stereotype.Repository


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
        val criteria = Criteria(SysTenantSystem::tenantId eq tenantId)
        return searchProperty(criteria, SysTenantSystem::systemCode).toSet()
    }

    /**
     * 根据系统编码查找对应的租户id
     *
     * @param systemCode 系统编码
     * @return Set<租户id>
     */
    fun searchTenantIdsBySystemCode(systemCode: String): Set<String> {
        val criteria = Criteria(SysTenantSystem::systemCode eq systemCode)
        return searchProperty(criteria, SysTenantSystem::tenantId).toSet()
    }

    /**
     * 根据租户id对系统编码进行分组
     *
     * @param tenantIds 查询条件：租户id集合，为null时将查出所有记录，默认为null
     * @return Map<租户id， List<系统编码>>
     */
    fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>> {
        val returnProperties = listOf(SysTenantSystem::tenantId, SysTenantSystem::systemCode)
        val results = if (tenantIds == null) {
            allSearchProperties(returnProperties)
        } else {
            val criteria = Criteria(SysTenantSystem::tenantId inList tenantIds)
            searchProperties(criteria, returnProperties)
        }
        val pairs = results.mapNotNull { row ->
            val tenantId = row[SysTenantSystem::tenantId.name] as? String
            val systemCode = row[SysTenantSystem::systemCode.name] as? String
            if (tenantId != null && systemCode != null) tenantId to systemCode else null
        }
        return pairs.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
        )
    }

    /**
     * 根据系统编码对租户id进行分组
     *
     * @param systemCodes 查询条件：系统编码集合，为null时将查出所有记录，默认为null
     * @return Map<系统编码， List<租户id>>
     */
    fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>? = null): Map<String, List<String>> {
        val returnProperties = listOf(SysTenantSystem::systemCode, SysTenantSystem::tenantId)
        val results = if (systemCodes == null) {
            allSearchProperties(returnProperties)
        } else {
            val criteria = Criteria(SysTenantSystem::systemCode inList systemCodes)
            searchProperties(criteria, returnProperties)
        }
        val pairs = results.mapNotNull { row ->
            val systemCode = row[SysTenantSystem::systemCode.name] as? String
            val tenantId = row[SysTenantSystem::tenantId.name] as? String
            if (systemCode != null && tenantId != null) systemCode to tenantId else null
        }
        return pairs.groupBy(
            keySelector = { it.first },
            valueTransform = { it.second }
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
        val criteria = Criteria.and(
            SysTenantSystem::tenantId eq tenantId,
            SysTenantSystem::systemCode eq systemCode
        )
        return count(criteria) > 0
    }

    /**
     * 按租户ID和系统编码删除关系
     *
     * @param tenantId 租户ID
     * @param systemCode 系统编码
     * @return 删除条数
     */
    fun deleteByTenantIdAndSystemCode(tenantId: String, systemCode: String): Int {
        val criteria = Criteria.and(
            SysTenantSystem::tenantId eq tenantId,
            SysTenantSystem::systemCode eq systemCode
        )
        return batchDeleteCriteria(criteria)
    }

    /**
     * 按租户ID集合批量删除关系
     *
     * @param tenantIds 租户ID集合
     * @return 删除条数
     */
    fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
        if (tenantIds.isEmpty()) {
            return 0
        }
        val criteria = Criteria(SysTenantSystem::tenantId inList tenantIds)
        return batchDeleteCriteria(criteria)
    }

    //endregion your codes 2

}
