package io.kudos.ms.auth.common.tenant.vo

import java.io.Serializable


/**
 * What a tenant bootstrap did.
 *
 * Reports created and pre-existing separately rather than a single count, because the two mean very
 * different things when a bootstrap is re-run: "created 0, existing 3" is the healthy idempotent
 * outcome, and a bare "0" would read as a failure.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class TenantBootstrapResult(

    /** The tenant. */
    val tenantId: String,

    /** Role codes created by this call. */
    val createdRoleCodes: List<String> = emptyList(),

    /** Role codes that were already present and were left untouched. */
    val existingRoleCodes: List<String> = emptyList(),

    /** Permission bindings created by this call. */
    val createdBindings: Int = 0,

    /** The role code an administrator was bound to, when this call appointed one. */
    val boundRoleCode: String? = null,

    /** The principal appointed, when this call appointed one. */
    val boundUserId: String? = null,

) : Serializable {
    companion object {
        private const val serialVersionUID = 1L
    }
}


/**
 * Whether a tenant has been bootstrapped, and by whom it is administered.
 *
 * One call rather than letting a console stitch "does the role exist" together with "who holds it":
 * the two questions have one answer here, and a UI assembling it from two endpoints would have to
 * invent what a missing role means — which is the difference between "not seeded yet" and "an
 * error", and the difference decides which button the operator needs.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class TenantBootstrapStatusVo(

    /** The tenant. */
    val tenantId: String,

    /** True when every configured bootstrap role exists. */
    val seeded: Boolean,

    /** The bootstrap role codes that exist for this tenant. */
    val presentRoleCodes: List<String> = emptyList(),

    /** The configured bootstrap role codes that are still missing. */
    val missingRoleCodes: List<String> = emptyList(),

    /** Who currently holds the primary bootstrap role. Empty means the tenant has nobody in charge. */
    val administrators: List<Administrator> = emptyList(),

) : Serializable {

    /** One holder of the bootstrap role. */
    data class Administrator(
        val userId: String,
        val username: String? = null,
    ) : Serializable {
        companion object {
            private const val serialVersionUID = 1L
        }
    }

    companion object {
        private const val serialVersionUID = 1L
    }
}
