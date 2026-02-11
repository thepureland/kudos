package io.kudos.ms.auth.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
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
        val criteria = Criteria.and(
            AuthGroupUser::groupId eq groupId,
            AuthGroupUser::userId eq userId
        )
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
        val criteria = Criteria(AuthGroupUser::groupId eq groupId)
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
        val criteria = Criteria(AuthGroupUser::userId eq userId)
        @Suppress("UNCHECKED_CAST")
        val groupIds = searchProperty(criteria, AuthGroupUser::groupId.name) as List<String>
        return groupIds.toSet()
    }

    /**
     * 全量用户组-用户关系，按用户ID分组为「用户ID -> 用户组ID列表」
     *
     * @return Map<用户id，List<组id>>
     */
    fun searchAllUserIdToGroupIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.groupId } }
    }

    /**
     * 全量用户组-用户关系，按用户组ID分组为「用户组ID -> 用户ID列表」
     *
     * @return Map<组ID, List<用户ID>>
     */
    fun searchAllGroupIdToUserIdsForCache(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.groupId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    /**
     * 按组ID和用户ID删除关系
     *
     * @param groupId 组ID
     * @param userId 用户ID
     * @return 删除条数
     */
    fun deleteByGroupIdAndUserId(groupId: String, userId: String): Int {
        val criteria = Criteria.and(
            AuthGroupUser::groupId eq groupId,
            AuthGroupUser::userId eq userId
        )
        return batchDeleteCriteria(criteria)
    }

    //endregion your codes 2

}
