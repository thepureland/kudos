package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
import io.kudos.ms.auth.core.model.po.AuthGroupUser
import io.kudos.ms.auth.core.model.table.AuthGroupUsers
import org.springframework.stereotype.Repository


/**
 * 组-用户关系数据访问对象
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class AuthGroupUserDao : BaseCrudDao<String, AuthGroupUser, AuthGroupUsers>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param groupId 组ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Codex
     * @since 1.0.0
     */
    fun exists(groupId: String, userId: String): Boolean {
        val criteria = Criteria.of(AuthGroupUser::groupId.name, OperatorEnum.EQ, groupId)
            .addAnd(AuthGroupUser::userId.name, OperatorEnum.EQ, userId)
        return count(criteria) > 0
    }

    /**
     * 根据组ID查询用户ID集合
     *
     * @param groupId 组ID
     * @return 用户ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchUserIdsByGroupId(groupId: String): Set<String> {
        val criteria = Criteria.of(AuthGroupUser::groupId.name, OperatorEnum.EQ, groupId)
        @Suppress("UNCHECKED_CAST")
        val userIds = searchProperty(criteria, AuthGroupUser::userId.name) as List<String>
        return userIds.toSet()
    }

    /**
     * 根据用户ID查询组ID集合
     *
     * @param userId 用户ID
     * @return 组ID集合
     * @author AI: Codex
     * @since 1.0.0
     */
    fun searchGroupIdsByUserId(userId: String): Set<String> {
        val criteria = Criteria.of(AuthGroupUser::userId.name, OperatorEnum.EQ, userId)
        @Suppress("UNCHECKED_CAST")
        val groupIds = searchProperty(criteria, AuthGroupUser::groupId.name) as List<String>
        return groupIds.toSet()
    }

    /** 按用户ID查询用户组ID列表（供 GroupIdsByUserIdCache 使用） */
    fun getGroupIdsByUserId(userId: String): List<String> = searchGroupIdsByUserId(userId).toList()

    /** 全量用户组-用户关系，按用户ID分组为「用户ID -> 用户组ID列表」（供 GroupIdsByUserIdCache.reloadAll） */
    fun getAllUserIdToGroupIdsForCache(): Map<String, List<String>> {
        @Suppress("UNCHECKED_CAST")
        val all = allSearch() as List<AuthGroupUser>
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.groupId } }
    }

    /** 按用户组ID查询用户ID列表（供 UserIdsByGroupIdCache 使用） */
    fun getUserIdsByGroupId(groupId: String): List<String> {
        val criteria = Criteria(AuthGroupUser::groupId.name, OperatorEnum.EQ, groupId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, AuthGroupUser::userId.name) as List<String>
    }

    /** 全量用户组-用户关系，按用户组ID分组为「用户组ID -> 用户ID列表」（供 UserIdsByGroupIdCache.reloadAll） */
    fun getAllGroupIdToUserIdsForCache(): Map<String, List<String>> {
        @Suppress("UNCHECKED_CAST")
        val all = allSearch() as List<AuthGroupUser>
        return all.groupBy { it.groupId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    //endregion your codes 2

}
