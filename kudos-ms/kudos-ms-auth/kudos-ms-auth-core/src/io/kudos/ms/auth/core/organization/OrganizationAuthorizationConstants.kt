package io.kudos.ms.auth.core.organization

/**
 * Per-tenant role override actions (G-1).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
enum class RoleOverrideAction {
    /** The role applies in this tenant although it is not a default role. */
    ADD,

    /** The default role does not apply in this tenant. */
    REMOVE,
}

/**
 * Built-in management roles (G-6, G-7). They are not business roles: they never take part in
 * effective-role calculation or tenant entry.
 */
enum class ManagementRoleKind {
    /** Manages members, departments, role definitions, defaults and every tenant's overrides. */
    ORGANIZATION_PERMISSION_ADMIN,

    /** Adds or removes existing business roles for members in the tenants it governs. */
    TENANT_PERMISSION_ADMIN,
}

/**
 * Rank of an actor towards one organization, highest first. A higher rank may manage lower ranks only.
 */
enum class OrganizationRank {
    PLATFORM,
    ORGANIZATION_ADMIN,
    ORGANIZATION_PERMISSION_ADMIN,
    TENANT_PERMISSION_ADMIN,
    MEMBER,

    /** Not a member of this organization at all. */
    NONE;

    /** Whether this rank is strictly above [other]. */
    fun outranks(other: OrganizationRank): Boolean = ordinal < other.ordinal
}
