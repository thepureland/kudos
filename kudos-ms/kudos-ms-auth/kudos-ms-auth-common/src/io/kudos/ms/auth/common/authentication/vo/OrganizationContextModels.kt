package io.kudos.ms.auth.common.authentication.vo

/** Scope an organization account's session works in. */
enum class SessionScope {
    /** One tenant of the organization, in one sub-system. */
    TENANT,

    /** The organization itself: management only, no tenant. */
    ORGANIZATION,
}

/** Where the current session works; [contextVersion] is the logical session id the console echoes back. */
data class SessionTarget(
    val organizationId: String,
    val scope: SessionScope,
    val tenantId: String?,
    val subSystemCode: String?,
    val contextVersion: String,
)

/** A tenant the account may enter, with the sub-systems it may use there. */
data class AvailableTenantContext(val tenantId: String, val name: String, val organizationId: String, val subSystemCodes: List<String>)

/** Every scope open to the current organization account. */
data class AvailableOrganizationContexts(
    val organizationId: String,
    val current: SessionTarget,
    /** Whether the organization scope (management) is open to the account. */
    val organizationScope: Boolean,
    val tenants: List<AvailableTenantContext>,
)

/**
 * Switch request: a tenant (with an optional sub-system), or the organization scope when [tenantId] is
 * blank. [contextVersion] must name the current session.
 */
data class OrganizationContextSwitchRequest(val tenantId: String? = null, val subSystemCode: String? = null, val contextVersion: String)

data class OrganizationContextSwitchUser(val id: String, val username: String, val organizationId: String, val tenantId: String)

data class OrganizationContextSwitchResult(val status: String = "SWITCHED", val current: SessionTarget, val user: OrganizationContextSwitchUser)
