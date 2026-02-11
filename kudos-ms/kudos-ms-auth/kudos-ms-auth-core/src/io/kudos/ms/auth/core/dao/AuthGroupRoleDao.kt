package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.Criterion
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.model.po.AuthGroupRole
import io.kudos.ms.auth.core.model.table.AuthGroupRoles
import org.springframework.stereotype.Repository


/**
 * 组-角色关系数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthGroupRoleDao : BaseCrudDao<String, AuthGroupRole, AuthGroupRoles>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param groupId 组ID
     * @param roleId 角色ID
     * @return 是否存在
     * @author AI: Codex
     * @since 1.0.0
     */
    fun exists(groupId: String, roleId: String): Boolean {
        val criteria = Criteria.and(
            Criterion(AuthGroupRole::groupId.name, OperatorEnum.EQ, groupId),
            Criterion(AuthGroupRole::roleId.name, OperatorEnum.EQ, roleId)
        )
        return count(criteria) > 0
    }

    /**
     * 根据组ID查询角色ID集合
     *
     * @param groupId 组ID
     * @return 角色ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchRoleIdsByGroupId(groupId: String): Set<String> {
        val criteria = Criteria(AuthGroupRole::groupId.name, OperatorEnum.EQ, groupId)
        @Suppress("UNCHECKED_CAST")
        val roleIds = searchProperty(criteria, AuthGroupRole::roleId.name) as List<String>
        return roleIds.toSet()
    }

    /**
     * 根据角色ID查询组ID集合
     *
     * @param roleId 角色ID
     * @return 组ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchGroupIdsByRoleId(roleId: String): Set<String> {
        val criteria = Criteria(AuthGroupRole::roleId.name, OperatorEnum.EQ, roleId)
        @Suppress("UNCHECKED_CAST")
        val groupIds = searchProperty(criteria, AuthGroupRole::groupId.name) as List<String>
        return groupIds.toSet()
    }

    /**
     * 全量用户组-角色关系，按用户组ID分组为「用户组ID -> 角色ID列表」
     *
     * @return Map<组id，List<角色id>>
     */
    fun searchAllGroupIdToRoleIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.groupId }.mapValues { (_, list) -> list.map { it.roleId } }
    }

    //endregion your codes 2

}
