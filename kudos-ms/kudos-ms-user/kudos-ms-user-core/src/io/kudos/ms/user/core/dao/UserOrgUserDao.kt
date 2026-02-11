package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.user.core.model.po.UserOrgUser
import io.kudos.ms.user.core.model.table.UserOrgUsers
import org.springframework.stereotype.Repository


/**
 * 机构-用户关系数据访问对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
//region your codes 1
open class UserOrgUserDao : BaseCrudDao<String, UserOrgUser, UserOrgUsers>() {
//endregion your codes 1

    //region your codes 2

    /**
     * 检查关系是否存在
     *
     * @param orgId 机构ID
     * @param userId 用户ID
     * @return 是否存在
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun exists(orgId: String, userId: String): Boolean {
        val criteria = Criteria.and(
            UserOrgUser::orgId eq orgId,
            UserOrgUser::userId eq userId
        )
        return count(criteria) > 0
    }

    /**
     * 按用户ID查询其所属机构ID列表
     */
    fun searchOrgIdsByUserId(userId: String): List<String> {
        val criteria = Criteria(UserOrgUser::userId eq userId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, UserOrgUser::orgId.name) as List<String>
    }

    /**
     * 按机构ID查询其下用户ID列表
     */
    fun searchUserIdsByOrgId(orgId: String): List<String> {
        val criteria = Criteria(UserOrgUser::orgId eq orgId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, UserOrgUser::userId.name) as List<String>
    }

    /**
     * 全量机构-用户关系，按用户ID分组为「用户ID -> 机构ID列表」
     */
    fun searchAllUserIdToOrgIds(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.orgId } }
    }

    /**
     * 全量机构-用户关系，按机构ID分组为「机构ID -> 用户ID列表」
     */
    fun searchAllOrgIdToUserIds(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.orgId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    /**
     * 按机构ID和用户ID删除关系
     *
     * @param orgId 机构ID
     * @param userId 用户ID
     * @return 删除条数
     */
    fun deleteByOrgIdAndUserId(orgId: String, userId: String): Int {
        val criteria = Criteria.and(
            UserOrgUser::orgId eq orgId,
            UserOrgUser::userId eq userId
        )
        return batchDeleteCriteria(criteria)
    }

    /**
     * 按机构ID和用户ID查询关系列表
     *
     * @param orgId 机构ID
     * @param userId 用户ID
     * @return 关系记录列表
     */
    fun searchByOrgIdAndUserId(orgId: String, userId: String): List<UserOrgUser> {
        val criteria = Criteria.and(
            UserOrgUser::orgId eq orgId,
            UserOrgUser::userId eq userId
        )
        return search(criteria)
    }

    /**
     * 按机构ID查询管理员用户ID列表
     *
     * @param orgId 机构ID
     * @return 管理员用户ID列表
     */
    fun searchAdminUserIdsByOrgId(orgId: String): List<String> {
        val criteria = Criteria(UserOrgUser::orgId eq orgId)
            .addAnd(UserOrgUser::orgAdmin eq true)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, UserOrgUser::userId.name) as List<String>
    }

    //endregion your codes 2

}
