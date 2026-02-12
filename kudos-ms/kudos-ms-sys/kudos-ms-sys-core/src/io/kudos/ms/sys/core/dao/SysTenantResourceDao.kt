package io.kudos.ms.sys.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.model.po.SysTenantResource
import io.kudos.ms.sys.core.model.table.SysTenantResources
import org.springframework.stereotype.Repository


/**
 * 租户-资源关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class SysTenantResourceDao : BaseCrudDao<String, SysTenantResource, SysTenantResources>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 根据租户id查找对应的资源id
     *
     * @param tenantId 租户id
     * @return Set<资源id>
     */
    fun searchResourceIdsByTenantId(tenantId: String): Set<String> {
        val criteria = Criteria(SysTenantResource::tenantId eq tenantId)
        return searchProperty(criteria, SysTenantResource::resourceId).toSet()
    }

    /**
     * 根据资源id查找对应的租户id
     *
     * @param resourceId 资源id
     * @return Set<租户id>
     */
    fun searchTenantIdsByResourceId(resourceId: String): Set<String> {
        val criteria = Criteria(SysTenantResource::resourceId eq resourceId)
        return searchProperty(criteria, SysTenantResource::tenantId).toSet()
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param resourceId 资源id
     * @return 是否存在
     */
    fun exists(tenantId: String, resourceId: String): Boolean {
        val criteria = Criteria.and(
            SysTenantResource::tenantId eq tenantId,
            SysTenantResource::resourceId eq resourceId
        )
        return count(criteria) > 0
    }

    /**
     * 按租户ID和资源ID删除关系
     *
     * @param tenantId 租户ID
     * @param resourceId 资源ID
     * @return 删除条数
     */
    fun deleteByTenantIdAndResourceId(tenantId: String, resourceId: String): Int {
        val criteria = Criteria.and(
            SysTenantResource::tenantId eq tenantId,
            SysTenantResource::resourceId eq resourceId
        )
        return batchDeleteCriteria(criteria)
    }

    //endregion your codes 2

}