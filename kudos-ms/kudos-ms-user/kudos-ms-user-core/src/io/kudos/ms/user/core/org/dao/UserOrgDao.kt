package io.kudos.ms.user.core.org.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.model.table.UserOrgs
import org.springframework.stereotype.Repository


/**
 * Organization data access object.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class UserOrgDao : BaseCrudDao<String, UserOrg, UserOrgs>() {


    /**
     * Queries by tenant ID and returns a list of cache VOs.
     *
     * @param tenantId tenant ID
     * @return List<UserOrgCacheEntry>
     */
    open fun searchOrgsByTenantIdForCache(tenantId: String): List<UserOrgCacheEntry> =
        searchAs<UserOrgCacheEntry>(Criteria(UserOrg::tenantId eq tenantId))

    /**
     * Queries the direct child organization ID list of the given parent organization in enabled status.
     *
     * @param parentId parent organization ID
     * @return list of direct child organization IDs
     */
    fun searchActiveChildOrgIds(parentId: String): List<String> {
        val criteria = Criteria(UserOrg::parentId eq parentId)
            .addAnd(UserOrg::active eq true)
        return searchProperty(criteria, UserOrg::id).filterNotNull()
    }

    /**
     * Queries enabled organizations under the tenant; when parentId is null, queries all enabled organizations.
     *
     * @param tenantId tenant ID
     * @param parentId parent organization ID; when null, do not filter by parent organization
     * @return organization list
     */
    fun searchActiveOrgsByTenantId(tenantId: String, parentId: String? = null): List<UserOrg> {
        val criteria = Criteria(UserOrg::tenantId eq tenantId).addAnd(UserOrg::active eq true)
        parentId?.let { criteria.addAnd(UserOrg::parentId eq it) }
        return search(criteria)
    }

    /**
     * Returns rootOrgId itself plus all recursively enabled descendant organization IDs.
     *
     * Used for IN-list expansion in "parent organization viewing child organization members" style queries.
     * Only includes child organizations with `active=true`, consistent with the filter scope of [searchActiveChildOrgIds].
     *
     * Performance: with tree depth N, performs N SELECTs. Sufficient for medium-sized organization trees
     * (< a few hundred nodes); for very large trees, switch to a single full-scan parentId reverse index
     * (see [searchAllOrgIdToParentId]) for batch computation.
     *
     * @return Set, including rootOrgId itself; rootOrgId itself is not necessarily active, callers should filter as needed.
     */
    fun searchOrgAndDescendantIds(rootOrgId: String): Set<String> {
        val result = linkedSetOf(rootOrgId)
        val queue = ArrayDeque<String>().apply { add(rootOrgId) }
        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            searchActiveChildOrgIds(current).forEach { childId ->
                if (result.add(childId)) queue.add(childId)
            }
        }
        return result
    }

    /**
     * Returns orgId itself plus all ancestor organization IDs (walking up parent_id to null or a cycle).
     *
     * Used for cache invalidation: when user relations under orgId change, the "includes child organization members"
     * view of all ancestor organizations needs to be recomputed.
     * Does not filter by active -- even if an ancestor is disabled, cache entries may still need to be cleared.
     */
    fun searchOrgAndAncestorIds(orgId: String): Set<String> {
        val result = linkedSetOf(orgId)
        var current: String? = get(orgId)?.parentId
        while (current != null && result.add(current)) {
            current = get(current)?.parentId
        }
        return result
    }

    /**
     * Full orgId -> parentId mapping for all organizations, used by cache batch warm-up
     * to build the parent-child index. A value of null indicates a root organization.
     */
    fun searchAllOrgIdToParentId(): Map<String, String?> =
        allSearch().associate { it.id to it.parentId }


}
