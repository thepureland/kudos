package io.kudos.ms.sys.core.tenant.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.sys.common.tenant.vo.SysTenantSystemCacheEntry
import io.kudos.ms.sys.core.tenant.model.po.SysTenantSystem
import io.kudos.ms.sys.core.tenant.model.table.SysTenantSystems
import org.springframework.stereotype.Repository


/**
 * 租户-系统关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class SysTenantSystemDao : BaseCrudDao<String, SysTenantSystem, SysTenantSystems>() {


    /**
     * 根据租户id查找对应的系统编码
     *
     * @param tenantId 租户id
     * @return Set<系统编码>
     */
    fun searchSystemCodesByTenantId(tenantId: String): Set<String> =
        searchProperty(Criteria(SysTenantSystem::tenantId eq tenantId), SysTenantSystem::systemCode).toSet()

    /**
     * 根据系统编码查找对应的租户id
     *
     * @param systemCode 系统编码
     * @return Set<租户id>
     */
    fun searchTenantIdsBySystemCode(systemCode: String): Set<String> =
        searchProperty(Criteria(SysTenantSystem::systemCode eq systemCode), SysTenantSystem::tenantId).toSet()

    /**
     * 根据租户id对系统编码进行分组
     *
     * @param tenantIds 查询条件：租户id集合，为null时将查出所有记录，默认为null
     * @return Map<租户id， List<系统编码>>
     */
    fun groupingSystemCodesByTenantIds(tenantIds: Collection<String>? = null): Map<String, List<String>> =
        groupRows(
            keyProp = SysTenantSystem::tenantId,
            valueProp = SysTenantSystem::systemCode,
            filter = tenantIds?.let { Criteria(SysTenantSystem::tenantId inList it) },
        )

    /**
     * 根据系统编码对租户id进行分组
     *
     * @param systemCodes 查询条件：系统编码集合，为null时将查出所有记录，默认为null
     * @return Map<系统编码， List<租户id>>
     */
    fun groupingTenantIdsBySystemCodes(systemCodes: Collection<String>? = null): Map<String, List<String>> =
        groupRows(
            keyProp = SysTenantSystem::systemCode,
            valueProp = SysTenantSystem::tenantId,
            filter = systemCodes?.let { Criteria(SysTenantSystem::systemCode inList it) },
        )

    private fun groupRows(
        keyProp: kotlin.reflect.KProperty1<SysTenantSystem, *>,
        valueProp: kotlin.reflect.KProperty1<SysTenantSystem, *>,
        filter: Criteria?,
    ): Map<String, List<String>> {
        val returnProperties = listOf(keyProp, valueProp)
        val results = if (filter == null) allSearchProperties(returnProperties)
            else searchProperties(filter, returnProperties)
        return results
            .mapNotNull { row ->
                val k = row[keyProp.name] as? String
                val v = row[valueProp.name] as? String
                if (k != null && v != null) k to v else null
            }
            .groupBy({ it.first }, { it.second })
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param systemCode 系统编码
     * @return 是否存在
     * @author AI: Cursor
     */
    fun exists(tenantId: String, systemCode: String): Boolean = count(
        Criteria.and(SysTenantSystem::tenantId eq tenantId, SysTenantSystem::systemCode eq systemCode)
    ) > 0

    /**
     * 按租户ID和系统编码删除关系
     *
     * @param tenantId 租户ID
     * @param systemCode 系统编码
     * @return 删除条数
     */
    fun deleteByTenantIdAndSystemCode(tenantId: String, systemCode: String): Int = batchDeleteCriteria(
        Criteria.and(SysTenantSystem::tenantId eq tenantId, SysTenantSystem::systemCode eq systemCode)
    )

    /**
     * 按租户ID集合批量删除关系
     *
     * @param tenantIds 租户ID集合
     * @return 删除条数
     */
    fun batchDeleteByTenantIds(tenantIds: Collection<String>): Int {
        if (tenantIds.isEmpty()) return 0
        return batchDeleteCriteria(Criteria(SysTenantSystem::tenantId inList tenantIds))
    }

    /**
     * 全量查询供 Hash 缓存加载
     *
     * @return 所有租户-系统关系缓存项
     */
    open fun fetchAllForCache(): List<SysTenantSystemCacheEntry> =
        searchAs<SysTenantSystemCacheEntry>(null)

    /**
     * 按系统编码查询供 Hash 缓存按副属性回写
     *
     * @param systemCode 系统编码
     * @return 该系统下的租户-系统关系缓存项列表
     */
    open fun fetchCacheItemsBySystemCode(systemCode: String): List<SysTenantSystemCacheEntry> =
        searchAs<SysTenantSystemCacheEntry>(Criteria(SysTenantSystem::systemCode eq systemCode))

    /**
     * 按租户id查询供 Hash 缓存按副属性回写
     *
     * @param tenantId 租户id
     * @return 该租户下的租户-系统关系缓存项列表
     */
    open fun fetchCacheItemsByTenantId(tenantId: String): List<SysTenantSystemCacheEntry> =
        searchAs<SysTenantSystemCacheEntry>(Criteria(SysTenantSystem::tenantId eq tenantId))


}
