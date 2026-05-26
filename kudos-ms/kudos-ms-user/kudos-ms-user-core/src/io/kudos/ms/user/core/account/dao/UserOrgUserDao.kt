package io.kudos.ms.user.core.account.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.user.core.account.model.po.UserOrgUser
import io.kudos.ms.user.core.account.model.table.UserOrgUsers
import org.springframework.stereotype.Repository


/**
 * Organization-user association DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class UserOrgUserDao : BaseCrudDao<String, UserOrgUser, UserOrgUsers>() {


    /**
     * Check whether the association exists.
     *
     * @param orgId organization id
     * @param userId user id
     * @return true if it exists
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
     * Query the organization ids a user belongs to.
     */
    fun searchOrgIdsByUserId(userId: String): List<String> {
        val criteria = Criteria(UserOrgUser::userId eq userId)
        return searchProperty(criteria, UserOrgUser::orgId)
    }

    /**
     * Query the user ids under an organization.
     */
    fun searchUserIdsByOrgId(orgId: String): List<String> {
        val criteria = Criteria(UserOrgUser::orgId eq orgId)
        return searchProperty(criteria, UserOrgUser::userId)
    }

    /**
     * Batch-query user ids under multiple organization ids (no dedup; callers should distinct as needed).
     * Used by IN-list expansion for "parent org includes child org members" queries.
     */
    fun searchUserIdsByOrgIds(orgIds: Collection<String>): List<String> {
        if (orgIds.isEmpty()) return emptyList()
        val criteria = Criteria(UserOrgUser::orgId inList orgIds.toList())
        return searchProperty(criteria, UserOrgUser::userId)
    }

    /**
     * All organization-user associations, grouped by userId as "userId -> list of orgIds".
     */
    fun searchAllUserIdToOrgIds(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.userId }.mapValues { (_, list) -> list.map { it.orgId } }
    }

    /**
     * All organization-user associations, grouped by orgId as "orgId -> list of userIds".
     */
    fun searchAllOrgIdToUserIds(): Map<String, List<String>> {
        val all = allSearch()
        return all.groupBy { it.orgId }.mapValues { (_, list) -> list.map { it.userId } }
    }

    /**
     * Delete the association by orgId and userId.
     *
     * @param orgId organization id
     * @param userId user id
     * @return number of rows deleted
     */
    fun deleteByOrgIdAndUserId(orgId: String, userId: String): Int {
        val criteria = Criteria.and(
            UserOrgUser::orgId eq orgId,
            UserOrgUser::userId eq userId
        )
        return batchDeleteCriteria(criteria)
    }

    /**
     * Query association records by orgId and userId.
     *
     * @param orgId organization id
     * @param userId user id
     * @return list of association records
     */
    fun searchByOrgIdAndUserId(orgId: String, userId: String): List<UserOrgUser> {
        val criteria = Criteria.and(
            UserOrgUser::orgId eq orgId,
            UserOrgUser::userId eq userId
        )
        return search(criteria)
    }

    /**
     * Query the admin user ids of an organization.
     *
     * @param orgId organization id
     * @return list of admin user ids
     */
    fun searchAdminUserIdsByOrgId(orgId: String): List<String> {
        val criteria = Criteria(UserOrgUser::orgId eq orgId)
            .addAnd(UserOrgUser::orgAdmin eq true)
        return searchProperty(criteria, UserOrgUser::userId)
    }


}
