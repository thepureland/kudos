package io.kudos.ams.auth.provider.dao

import io.kudos.ams.auth.provider.model.po.AuthRoleResource
import io.kudos.ams.auth.provider.model.table.AuthRoleResources
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import org.springframework.stereotype.Repository
import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao


/**
 * 角色-资源关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleResourceDao : BaseCrudDao<String, AuthRoleResource, AuthRoleResources>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param roleId 角色ID
     * @param resourceId 资源ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, resourceId: String): Boolean {
        val criteria = Criteria.of(AuthRoleResource::roleId.name, OperatorEnum.EQ, roleId)
            .addAnd(AuthRoleResource::resourceId.name, OperatorEnum.EQ, resourceId)
        return count(criteria) > 0
    }

    /**
     * 根据资源ID查询角色ID集合
     *
     * @param resourceId 资源ID
     * @return 角色ID集合
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun searchRoleIdsByResourceId(resourceId: String): Set<String> {
        val criteria = Criteria.of(AuthRoleResource::resourceId.name, OperatorEnum.EQ, resourceId)
        @Suppress("UNCHECKED_CAST")
        val roleIds = searchProperty(criteria, AuthRoleResource::roleId.name) as List<String>
        return roleIds.toSet()
    }

    //endregion your codes 2

}
