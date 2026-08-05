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
 * @author AI: Claude
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
     */
    fun searchActiveRoleIdsByTenantId(tenantId: String): List<String> {
        val criteria = Criteria(AuthRole::tenantId eq tenantId)
            .addAnd(AuthRole::active eq true)
        return searchProperty(criteria, AuthRole::id).filterNotNull()
    }

    /**
     * Returns the ids of every currently active role.
     *
     * Bulk counterpart of [filterActiveRoleIds], for the cache reload paths that expand thousands of
     * users in one pass and must not issue a query per user.
     *
     * @return the id set of all roles with `active = true`
     */
    open fun searchActiveRoleIdSet(): Set<String> {
        val criteria = Criteria(AuthRole::active eq true)
        return searchProperty(criteria, AuthRole::id).filterNotNull().toSet()
    }

    /**
     * Narrows [roleIds] to the subset that is currently active.
     *
     * **Deactivation semantics** (the single definition for the whole module): `active = false`
     * removes *only that role's own contribution* from every holder's effective permission set. It
     * does **not** sever the parent chain — a deactivated role still relays its ancestors to the
     * roles below it, so switching off one node never silently strips an unrelated grandparent's
     * resources from its descendants' holders. Concretely: the parent walk
     * ([searchAncestorRoleIds]) runs over the raw graph, and this filter is applied to the
     * *resulting set*, never during the walk.
     *
     * Ids that no longer exist are dropped as well (a dangling relation grants nothing).
     *
     * @param roleIds candidate role ids (may contain ids of deleted roles)
     * @return the subset that exists and is active; empty when [roleIds] is empty
     */
    open fun filterActiveRoleIds(roleIds: Collection<String>): Set<String> {
        if (roleIds.isEmpty()) return emptySet()
        val criteria = Criteria.and(
            AuthRole::id inList roleIds.toSet(),
            AuthRole::active eq true,
        )
        return searchProperty(criteria, AuthRole::id).filterNotNull().toSet()
    }

    /**
     * Bulk-cache helper: every role with a non-null parent_id as a `roleId -> parentId` map.
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
     * role IDs (excluding the inputs themselves). Batched per generation; capped at [maxDepth].
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
        ancestors.removeAll(roleIds.toSet())
        return ancestors
    }

    /**
     * Walks down the parent_id graph from [rootId] and returns all descendant role IDs.
     * Used for cycle prevention on parent_id mutations.
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

    /**
     * Takes an exclusive row lock on the role and holds it until the surrounding transaction ends.
     *
     * The role-scoped sibling of `AuthPrincipalVersionDao.lockPrincipal`, for read-then-write
     * sequences whose invariant is a property of the **role**, not of any principal — "this role has
     * no holders yet" being the motivating case: two concurrent first-administrator appointments
     * bind *different* principals, so the per-principal locks never collide, and both observe the
     * empty holder set. Same mechanics as lockPrincipal and for the same reason: an UPDATE rather
     * than `SELECT ... FOR UPDATE`, because the select form did not serialise writers on H2's
     * MVStore, and a lock that silently does not lock is worse than none.
     *
     * No create-retry loop is needed here — the caller has the role in hand, so its row exists; zero
     * rows touched means it was deleted out from under us, which must fail the write anyway.
     *
     * Must be called inside a transaction; the lock is released when it commits or rolls back.
     */
    open fun lockRole(roleId: String) {
        require(roleId.isNotBlank()) { "roleId must not be blank." }
        val touched = database().useConnection { connection ->
            connection.prepareStatement(
                """update "auth_role" set "update_time" = ? where "id" = ?""",
            ).use { statement ->
                statement.setTimestamp(1, java.sql.Timestamp.valueOf(java.time.LocalDateTime.now()))
                statement.setString(2, roleId)
                statement.executeUpdate()
            }
        }
        check(touched > 0) { "role ${roleId} no longer exists; the write is refused." }
    }
}
