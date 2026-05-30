package io.kudos.ms.auth.common.exclusion.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * Request VO for creating an SoD exclusion pair.
 *
 * The service layer canonicalises role pair order (smaller id → roleAId), so callers
 * do not need to worry about ordering — (A,B) and (B,A) are treated as the same pair.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class AuthRoleExclusionFormCreate(

    /** One role in the mutually exclusive pair. */
    val roleAId: String?,

    /** The other role in the pair. */
    val roleBId: String?,

    /** Tenant id. Both roles must belong to this tenant. */
    val tenantId: String?,

    /** Optional human-readable reason, e.g. "Auditor may not also approve payments". */
    @get:MaxLength(256)
    val description: String? = null,
)
