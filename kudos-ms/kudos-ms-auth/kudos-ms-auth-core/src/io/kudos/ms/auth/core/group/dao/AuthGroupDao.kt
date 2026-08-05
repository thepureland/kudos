package io.kudos.ms.auth.core.group.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.auth.common.group.vo.AuthGroupCacheEntry
import io.kudos.ms.auth.core.group.model.po.AuthGroup
import io.kudos.ms.auth.core.group.model.table.AuthGroups
import org.springframework.stereotype.Repository


/**
 * User group DAO.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthGroupDao : BaseCrudDao<String, AuthGroup, AuthGroups>() {


    /**
     * Returns all user groups with active=true.
     *
     * @return list of AuthGroupCacheEntry
     */
    open fun searchActiveGroupsForCache(): List<AuthGroupCacheEntry> {
        val criteria = Criteria(AuthGroup::active eq true)
        return searchAs(criteria)
    }

    /**
     * Returns the ids of every currently active group.
     *
     * Bulk counterpart of [filterActiveGroupIds], for cache reload paths that expand every user in
     * one pass and must not issue a query per user.
     *
     * @return the id set of all groups with `active = true`
     */
    open fun searchActiveGroupIdSet(): Set<String> {
        val criteria = Criteria(AuthGroup::active eq true)
        return searchProperty(criteria, AuthGroup::id).filterNotNull().toSet()
    }

    /**
     * Narrows [groupIds] to the subset that is currently active.
     *
     * A deactivated group relays no roles to its members — groups are a flat container, so unlike
     * roles (see [io.kudos.ms.auth.core.role.dao.AuthRoleDao.filterActiveRoleIds]) there is no chain
     * to preserve. Ids that no longer exist are dropped as well.
     *
     * @param groupIds candidate group ids (may contain ids of deleted groups)
     * @return the subset that exists and is active; empty when [groupIds] is empty
     */
    open fun filterActiveGroupIds(groupIds: Collection<String>): Set<String> {
        if (groupIds.isEmpty()) return emptySet()
        val criteria = Criteria.and(
            AuthGroup::id inList groupIds.toSet(),
            AuthGroup::active eq true,
        )
        return searchProperty(criteria, AuthGroup::id).filterNotNull().toSet()
    }

    /**
     * Looks up a single cache VO by tenant and group code (regardless of active flag).
     */
    open fun searchGroupByTenantIdAndGroupCode(tenantId: String, code: String): AuthGroupCacheEntry? {
        val criteria = Criteria.and(
            AuthGroup::tenantId eq tenantId,
            AuthGroup::code eq code
        )
        return searchAs<AuthGroupCacheEntry>(criteria).firstOrNull()
    }


}
