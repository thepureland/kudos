package io.kudos.ms.auth.core.role.exclusion.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.auth.common.exclusion.vo.response.AuthRoleExclusionViolationVo
import io.kudos.ms.auth.core.role.exclusion.model.po.AuthRoleExclusion

/**
 * Service interface for SoD mutual-exclusion pairs.
 *
 * **Canonical ordering**: every pair is stored with the lexicographically smaller role id in
 * [AuthRoleExclusion.roleAId]. The service layer enforces this on [insert] so callers do not
 * need to care about order — (A,B) and (B,A) are treated as the same constraint.
 *
 * **Validation**: before a new pair is persisted the service verifies:
 *  - Both role IDs are non-blank and not equal to each other.
 *  - Both roles exist in the same tenant.
 *  - The canonical pair does not already exist for the tenant.
 *  - (Lax) existing user assignments are NOT checked at creation time; instead,
 *    admins discover existing violations via [findViolatingUserIds].
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface IAuthRoleExclusionService : IBaseCrudService<String, AuthRoleExclusion> {

    /**
     * Returns all exclusion pairs that involve at least one role in [roleIds].
     * Used by the bind-validation hook for a fast batch lookup.
     */
    fun findByRoleIds(roleIds: Collection<String>): List<AuthRoleExclusion>

    /**
     * Finds users who currently violate the exclusion with [exclusionId] by holding both
     * sides of the pair (via direct assignment, group membership, or parent inheritance).
     *
     * Intended for the admin "SoD violations dashboard". Not called on the hot bind path
     * (the bind validator is forward-looking and rejects only new violations).
     */
    fun findViolatingUserIds(exclusionId: String): AuthRoleExclusionViolationVo

    /**
     * Checks whether adding [candidateRoleId] to a user who already has [existingRoleIds]
     * (effective roles — already expanded with groups + ancestors) would violate any SoD rule
     * in the same tenant.
     *
     * Returns the first violating exclusion found, or null if no violation.
     *
     * @param tenantId         Tenant scope (only exclusions in this tenant are considered).
     * @param candidateRoleId  The role being added.
     * @param existingRoleIds  The user's current effective role set.
     */
    fun findViolation(
        tenantId: String,
        candidateRoleId: String,
        existingRoleIds: Collection<String>,
    ): AuthRoleExclusion?
}
