package io.kudos.ms.user.core.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.enums.OperatorEnum
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
        val criteria = Criteria.of(UserOrgUser::orgId.name, OperatorEnum.EQ, orgId)
            .addAnd(UserOrgUser::userId.name, OperatorEnum.EQ, userId)
        return count(criteria) > 0
    }

    /**
     * 按用户ID查询其所属机构ID列表（供 OrgIdsByUserIdCache 使用）
     */
    fun getOrgIdsByUserId(userId: String): List<String> {
        val criteria = Criteria(UserOrgUser::userId.name, OperatorEnum.EQ, userId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, UserOrgUser::orgId.name) as List<String>
    }

    /**
     * 按机构ID查询其下用户ID列表（供 UserIdsByOrgIdCache 使用）
     */
    fun getUserIdsByOrgId(orgId: String): List<String> {
        val criteria = Criteria(UserOrgUser::orgId.name, OperatorEnum.EQ, orgId)
        @Suppress("UNCHECKED_CAST")
        return searchProperty(criteria, UserOrgUser::userId.name) as List<String>
    }

    /**
     * 全量机构-用户关系，按用户ID分组为「用户ID -> 机构ID列表」（供 OrgIdsByUserIdCache.reloadAll）
     */
    fun getAllUserIdToOrgIdsForCache(): Map<String, List<String>> {
        @Suppress("UNCHECKED_CAST")
        val all = allSearch() as List<UserOrgUser>
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.orgId } }
    }

    /**
     * 全量机构-用户关系，按机构ID分组为「机构ID -> 用户ID列表」（供 UserIdsByOrgIdCache.reloadAll）
     */
    fun getAllOrgIdToUserIdsForCache(): Map<String, List<String>> {
        @Suppress("UNCHECKED_CAST")
        val all = allSearch() as List<UserOrgUser>
        return all.groupBy { it.orgId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    //endregion your codes 2

}
