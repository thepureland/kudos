package io.kudos.ms.auth.common.authz.enums


/**
 * What a permission binding does when it matches.
 *
 * Stored in `auth_role_resource.effect`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
enum class PermissionEffect {

    /** Grants the permission. */
    ALLOW,

    /**
     * Withholds it. A matching DENY beats every ALLOW and **cannot be overridden by a narrower
     * rule** — a deliberate trade of expressiveness for explainability: "why was I denied" then has
     * exactly one answer (this rule), instead of requiring the reader to rank overlapping patterns
     * in their head.
     */
    DENY,
    ;

    companion object {

        /** Parse a stored value; unrecognised or blank input falls back to [ALLOW]. */
        @JvmStatic
        fun fromCode(code: String?): PermissionEffect =
            entries.firstOrNull { it.name.equals(code?.trim(), ignoreCase = true) } ?: ALLOW
    }
}
