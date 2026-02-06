package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.model.po.AuthRoleUser
import io.kudos.ms.auth.core.model.table.AuthRoleUsers
import org.springframework.stereotype.Repository


/**
 * 角色-用户关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthRoleUserDao : BaseCrudDao<String, AuthRoleUser, AuthRoleUsers>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(roleId: String, userId: String): Boolean {
        val criteria = Criteria.of(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
            .addAnd(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        return count(criteria) > 0
    }

    /** 按用户ID查询角色ID列表（供 RoleIdsByUserIdCache 使用） */
    fun getRoleIdsByUserId(userId: String): List<String> {
        val criteria = Criteria(AuthRoleUser::userId.name, OperatorEnum.EQ, userId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, AuthRoleUser::roleId.name) as List<String>
    }

    /** 全量角色-用户关系，按用户ID分组为「用户ID -> 角色ID列表」（供 RoleIdsByUserIdCache.reloadAll） */
    fun getAllUserIdToRoleIdsForCache(): Map<String, List<String>> {
        @Suppress("UNCHECKED_CAST")
        val all = allSearch() as List<AuthRoleUser>
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.roleId } }
    }

    /** 按角色ID查询用户ID列表（供 ResourceIdsByTenantIdAndUsernameCache.syncOnRoleResourceChange、UserIdsByRoleIdCache 等使用） */
    fun getUserIdsByRoleId(roleId: String): List<String> {
        val criteria = Criteria(AuthRoleUser::roleId.name, OperatorEnum.EQ, roleId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, AuthRoleUser::userId.name) as List<String>
    }

    /** 全量角色-用户关系，按角色ID分组为「角色ID -> 用户ID列表」（供 UserIdsByRoleIdCache / UserIdsByTenantIdAndRoleCodeCache.reloadAll） */
    fun getAllRoleIdToUserIdsForCache(): Map<String, List<String>> {
        @Suppress("UNCHECKED_CAST")
        val all = allSearch() as List<AuthRoleUser>
        return all.groupBy { it.roleId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    //endregion your codes 2

}
