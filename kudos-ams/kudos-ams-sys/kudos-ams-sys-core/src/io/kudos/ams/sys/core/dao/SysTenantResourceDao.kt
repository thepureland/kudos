package io.kudos.ams.sys.core.dao

import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ams.sys.core.model.po.SysTenantResource
import io.kudos.ams.sys.core.model.table.SysTenantResources
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


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
        val criteria = Criteria.of(SysTenantResource::tenantId.name, OperatorEnum.EQ, tenantId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantResource::resourceId.name).toSet() as Set<String>
    }

    /**
     * 根据资源id查找对应的租户id
     *
     * @param resourceId 资源id
     * @return Set<租户id>
     */
    fun searchTenantIdsByResourceId(resourceId: String): Set<String> {
        val criteria = Criteria.of(SysTenantResource::resourceId.name, OperatorEnum.EQ, resourceId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, SysTenantResource::tenantId.name).toSet() as Set<String>
    }

    /**
     * 检查关系是否存在
     *
     * @param tenantId 租户id
     * @param resourceId 资源id
     * @return 是否存在
     * @author AI: Cursor
     */
    fun exists(tenantId: String, resourceId: String): Boolean {
        val criteria = Criteria.of(SysTenantResource::tenantId.name, OperatorEnum.EQ, tenantId)
            .addAnd(SysTenantResource::resourceId.name, OperatorEnum.EQ, resourceId)
        return count(criteria) > 0
    }

    //endregion your codes 2

}