package io.kudos.ms.auth.core.role.exclusion.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.base.query.inList
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion
import io.kudos.ms.auth.core.role.exclusion.model.table.AuthRoleExclusions
import org.springframework.stereotype.Repository

/**
 * DAO for SoD mutual-exclusion pairs.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Repository
open class AuthRoleExclusionDao : BaseCrudDao<String, AuthRoleExclusion, AuthRoleExclusions>() {

    /**
     * Find all exclusion pairs where either side of the pair is in [roleIds].
     *
     * Used by the validation hook: given a user's effective role set, retrieve every
     * SoD constraint that involves any of those roles, then check for conflicts.
     *
     * Two separate IN queries are issued (one per column) and the results are unioned.
     * An OR IN across two columns cannot use separate indexes efficiently on H2 or Postgres,
     * so the UNION approach gives the optimizer a chance to use both indexes.
     *
     * @param roleIds Role IDs to look up (as either side of a pair).
     * @return All exclusion rows involving at least one id from [roleIds].
     */
    open fun searchByRoleIds(roleIds: Collection<String>): List<AuthRoleExclusion> {
        if (roleIds.isEmpty()) return emptyList()
        val byA = search(Criteria(AuthRoleExclusion::roleAId inList roleIds))
        val byB = search(Criteria(AuthRoleExclusion::roleBId inList roleIds))
        // Deduplicate by id (overlap when both sides are in roleIds).
        val seen = HashSet<String>()
        return (byA + byB).filter { row ->
            val id = row.id
            if (seen.contains(id)) false else { seen.add(id); true }
        }
    }

    /**
     * Find all exclusion pairs for a tenant involving a specific role on **either** side. Backs both
     * the admin "constraints for this role" view and the separation-of-duties check on the bind path.
     *
     * Each side gets its own [Criteria] on purpose: `addAnd` mutates the receiver and returns it, so
     * threading one instance through both queries would AND the two role predicates together
     * (`role_a_id = X AND role_b_id = X`) — a shape no row can satisfy, since self-exclusion is
     * forbidden by a check constraint. The second query then returned nothing and every rule whose
     * canonical ordering put this role in the B slot went unenforced.
     */
    open fun searchByRoleIdAndTenant(roleId: String, tenantId: String): List<AuthRoleExclusion> {
        // Each query needs its OWN Criteria: addAnd mutates and returns the SAME instance, so reusing
        // one would carry the roleAId condition into the byB query (tenant AND roleAId=roleId AND
        // roleBId=roleId — impossible), silently dropping every pair whose roleBId == roleId. That
        // bypassed the SoD check whenever the candidate role was the canonically-larger side.
        val byA = search(Criteria.and(AuthRoleExclusion::tenantId eq tenantId, AuthRoleExclusion::roleAId eq roleId))
        val byB = search(Criteria.and(AuthRoleExclusion::tenantId eq tenantId, AuthRoleExclusion::roleBId eq roleId))
        val seen = HashSet<String>()
        return (byA + byB).filter { seen.add(it.id) }
    }

    /**
     * Check whether a canonical pair (a < b) already exists for the tenant.
     * Called before insert to produce a friendly error instead of a constraint violation.
     */
    open fun pairExistsForTenant(roleAId: String, roleBId: String, tenantId: String): Boolean {
        val criteria = Criteria.and(
            AuthRoleExclusion::roleAId eq roleAId,
            AuthRoleExclusion::roleBId eq roleBId,
            AuthRoleExclusion::tenantId eq tenantId,
        )
        return search(criteria).isNotEmpty()
    }

    /**
     * Deletes every exclusion pair involving the role (on either side). Used by the role-delete
     * cascade: an exclusion rule that references a removed role is meaningless and would otherwise
     * linger as a dead constraint.
     *
     * Two criteria deletes are issued (one per column) because an OR across the two indexed
     * columns cannot use both indexes efficiently — same rationale as [searchByRoleIds].
     *
     * @param roleId role id
     * @return number of rows deleted
     */
    open fun deleteByRoleId(roleId: String): Int {
        val byA = batchDeleteCriteria(Criteria(AuthRoleExclusion::roleAId eq roleId))
        val byB = batchDeleteCriteria(Criteria(AuthRoleExclusion::roleBId eq roleId))
        return byA + byB
    }
}
