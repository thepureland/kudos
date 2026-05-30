package io.kudos.ms.auth.common.datascope.enums

/**
 * Data-scope (数据权限) policy attached to a role: which rows of business data a holder may access.
 *
 * Resolution across a user's effective roles takes the MOST permissive scope (see
 * IAuthRoleDataScopeService.resolveUserDataScope). A null/blank stored value is treated as [ALL]
 * for backward compatibility (roles that predate the feature must not be silently tightened).
 *
 * Stored in `auth_role.data_scope` as the [code] string.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
enum class DataScopeEnum(val code: String) {

    /** No row restriction — see all data in the tenant. Most permissive. */
    ALL("ALL"),

    /** The user's own org plus every descendant org. */
    ORG_AND_CHILD("ORG_AND_CHILD"),

    /** The user's own org only. */
    ORG("ORG"),

    /** Only rows the user created. Most restrictive (no org grant). */
    SELF("SELF"),

    /** An explicit set of orgs, listed in `auth_role_org`. */
    CUSTOM("CUSTOM"),
    ;

    companion object {
        /** Parse a wire/stored string into the enum, or null if unrecognised. */
        @JvmStatic
        fun fromCode(code: String?): DataScopeEnum? =
            code?.let { raw -> entries.firstOrNull { it.code == raw.trim().uppercase() } }
    }
}
