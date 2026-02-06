package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.model.po.AuthRoleResource
import io.kudos.ms.auth.core.model.table.AuthRoleResources
import org.springframework.stereotype.Repository


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

    /** 按角色ID查询资源ID列表（供 ResourceIdsByRoleIdCache 使用） */
    fun getResourceIdsByRoleId(roleId: String): List<String> = getResourceIdsByRoleIds(listOf(roleId))

    /** 按角色ID列表查询资源ID列表（去重，供 ResourceIdsByTenantIdAndUsernameCache 等使用） */
    fun getResourceIdsByRoleIds(roleIds: Collection<String>): List<String> {
        if (roleIds.isEmpty()) return emptyList()
        val criteria = Criteria(AuthRoleResource::roleId.name, OperatorEnum.IN, roleIds.toList())
        @Suppress("UNCHECKED_CAST")
        val list = searchProperty(criteria, AuthRoleResource::resourceId.name) as List<String>
        return list.map { it.trim() }.distinct()
    }

    /** 全量角色-资源关系，按角色ID分组为「角色ID -> 资源ID列表」（供 ResourceIdsByTenantIdAndUsernameCache.reloadAll） */
    fun getAllRoleIdToResourceIdsForCache(): Map<String, List<String>> {
        @Suppress("UNCHECKED_CAST")
        val all = allSearch() as List<AuthRoleResource>
        return all.groupBy { it.roleId }.mapValues { (_, list) -> list.map { it.resourceId.trim() } }
    }

    //endregion your codes 2

}
