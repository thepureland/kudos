package io.kudos.ms.auth.core.role.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.base.query.isNotNull
import io.kudos.ms.auth.common.role.vo.AuthRoleCacheEntry
import io.kudos.ms.auth.core.role.model.po.AuthRole
import io.kudos.ms.auth.core.role.model.table.AuthRoles
import org.springframework.stereotype.Repository


/**
 * Role DAO.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
@Repository
open class AuthRoleDao : BaseCrudDao<String, AuthRole, AuthRoles>() {



    /**
     * Queries all roles with active=true.
     *
     * @return List<AuthRoleCacheEntry>
     */
    open fun searchActiveRolesForCache(): List<AuthRoleCacheEntry> {
        val criteria = Criteria(AuthRole::active eq true)
        return searchAs<AuthRoleCacheEntry>(criteria)
    }

    /**
     * Queries by tenant and role code (regardless of active state) and returns a single cache VO.
     *
     * @param tenantId tenant id
     * @param code role code
     * @return AuthRoleCacheEntry, or null when not found
     */
    open fun searchRoleByTenantIdAndRoleCode(tenantId: String, code: String): AuthRoleCacheEntry? {
        val criteria = Criteria.and(
            AuthRole::tenantId eq tenantId,
            AuthRole::code eq code
        )
        return searchAs<AuthRoleCacheEntry>(criteria).firstOrNull()
    }

    /**
     * Queries all active role IDs under a tenant.
     *
     * @param tenantId tenant id
     * @return list of role IDs
     */
    fun searchActiveRoleIdsByTenantId(tenantId: String): List<String> {
        val criteria = Criteria(AuthRole::tenantId eq tenantId)
            .addAnd(AuthRole::active eq true)
        return searchProperty(criteria, AuthRole::id).filterNotNull()
    }

    /**
     * Bulk-cache helper: every role with a non-null parent_id as a `roleId -> parentId` map.
     * Used by ResourceIdsByUserIdCache's reloadAll path to expand effective roles with parents
     * in memory rather than per-user.
     */
    open fun searchAllRoleIdToParentIdForCache(): Map<String, String> {
        val criteria = Criteria(AuthRole::parentId.isNotNull())
        val rows = search(criteria)
        return rows.asSequence()
            .filter { it.parentId != null && it.id.isNotBlank() }
            .associate { it.id to it.parentId!! }
    }

    /**
     * Walks up the parent chain from each id in [roleIds] and returns the union of all ancestor
     * role IDs (excluding the inputs themselves). De-duplicated.
     *
     * Implementation note: rather than emit one SELECT per parent hop, batch each generation —
     * fetch all parents of the current frontier in a single criteria query, then repeat with the
     * newly-seen ancestors as the next frontier. Terminates when no new ancestors are produced
     * (steady state) or when the cap is hit (safety net against an unexpected cycle that slipped
     * past the service-level guard).
     *
     * @param roleIds Starting role ids.
     * @param maxDepth Safety cap on parent-chain depth. RBAC hierarchies that exceed this are
     *  almost certainly misconfigured; the cap also defends against a corrupted database that
     *  somehow contains a cycle.
     * @return Distinct ancestor role ids; never contains any id from [roleIds].
     */
    open fun searchAncestorRoleIds(roleIds: Collection<String>, maxDepth: Int = 32): Set<String> {
        if (roleIds.isEmpty()) return emptySet()
        val seen = HashSet<String>(roleIds)
        val ancestors = HashSet<String>()
        var frontier: Set<String> = roleIds.toSet()
        var depth = 0
        while (frontier.isNotEmpty() && depth < maxDepth) {
            val criteria = Criteria.and(
                AuthRole::id inList frontier,
                AuthRole::parentId.isNotNull(),
            )
            val parents = searchProperty(criteria, AuthRole::parentId).filterNotNull().toSet()
            val newOnes = parents.filterNot { seen.contains(it) }.toSet()
            if (newOnes.isEmpty()) break
            seen.addAll(newOnes)
            ancestors.addAll(newOnes)
            frontier = newOnes
            depth++
        }
        // Strip the inputs in case any of them appeared as an ancestor of a sibling (which would
        // imply an existing cycle; we still return only true ancestors).
        ancestors.removeAll(roleIds.toSet())
        return ancestors
    }

    /**
     * Walks down the parent_id graph from [rootId] and returns the union of all descendant role
     * IDs (excluding [rootId] itself). Used for the cycle check on parent-id updates: if the
     * proposed new parent is in the descendants of the role being updated, accepting the change
     * would close a cycle.
     *
     * Same batched-by-generation strategy as [searchAncestorRoleIds].
     *
     * @param rootId Role id whose subtree to walk.
     * @param maxDepth Safety cap; same rationale as [searchAncestorRoleIds].
     */
    open fun searchDescendantRoleIds(rootId: String, maxDepth: Int = 32): Set<String> {
        val seen = HashSet<String>().apply { add(rootId) }
        val descendants = HashSet<String>()
        var frontier: Set<String> = setOf(rootId)
        var depth = 0
        while (frontier.isNotEmpty() && depth < maxDepth) {
            val criteria = Criteria(AuthRole::parentId inList frontier)
            val children = searchProperty(criteria, AuthRole::id).filterNotNull().toSet()
            val newOnes = children.filterNot { seen.contains(it) }.toSet()
            if (newOnes.isEmpty()) break
            seen.addAll(newOnes)
            descendants.addAll(newOnes)
            frontier = newOnes
            depth++
        }
        return descendants
    }


}
