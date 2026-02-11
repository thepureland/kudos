package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
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
        val criteria = Criteria.and(
            AuthRoleUser::roleId eq roleId,
            AuthRoleUser::userId eq userId
        )
        return count(criteria) > 0
    }

    /**
     * 按用户ID查询角色ID列表
     *
     * @param userId 用户id
     * @return List<角色ID>
     */
    fun searchRoleIdsByUserId(userId: String): List<String> {
        val criteria = Criteria(AuthRoleUser::userId eq userId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, AuthRoleUser::roleId.name) as List<String>
    }

    /**
     * 全量角色-用户关系，按用户ID分组为「用户ID -> 角色ID列表」
     *
     * @return Map<用户ID,List<角色ID>>
     */
    fun searchAllUserIdToRoleIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.roleId } }
    }

    /**
     * 按角色ID查询用户ID列表
     *
     * @param roleId 角色id
     * @return List<用户ID>
     */
    fun searchUserIdsByRoleId(roleId: String): List<String> {
        val criteria = Criteria(AuthRoleUser::roleId eq roleId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, AuthRoleUser::userId.name) as List<String>
    }

    /**
     * 全量角色-用户关系，按角色ID分组为「角色ID -> 用户ID列表」
     *
     * @return Map<角色ID,List<用户ID>>
     */
    fun getAllRoleIdToUserIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.roleId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    /**
     * 按角色ID和用户ID删除关系
     *
     * @param roleId 角色ID
     * @param userId 用户ID
     * @return 删除条数
     */
    fun deleteByRoleIdAndUserId(roleId: String, userId: String): Int {
        val criteria = Criteria.and(
            AuthRoleUser::roleId eq roleId,
            AuthRoleUser::userId eq userId
        )
        return batchDeleteCriteria(criteria)
    }

    //endregion your codes 2

}
