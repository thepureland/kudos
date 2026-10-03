package io.kudos.ms.user.core.security

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.sys.core.organization.init.OrganizationModeProperties
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.common.security.IPlatformAdministratorPolicy
import io.kudos.ms.user.core.org.model.po.UserOrg
import io.kudos.ms.user.core.org.service.impl.OrganizationDirectoryGate
import io.kudos.ms.user.core.org.service.impl.UserOrgNodeKind
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import io.kudos.ms.user.core.org.spi.IOrganizationMemberPolicy
import org.springframework.mock.env.MockEnvironment

/*
 * Shared fixtures for the organization-mode pure tests (no Spring context, no database).
 * Names are distinct from the older `bindPrincipal` / `inject` helpers of this package.
 */

internal const val PLATFORM_TENANT = "platform"
internal const val PLATFORM_ADMIN = "platform-operator"

/** An [OrganizationMode] built directly from its properties and a mock environment. */
internal fun organizationMode(enabled: Boolean, platformTenants: Set<String> = setOf(PLATFORM_TENANT)): OrganizationMode =
    OrganizationMode(
        OrganizationModeProperties().apply { this.enabled = enabled },
        MockEnvironment().withProperty(OrganizationMode.PLATFORM_TENANT_IDS_KEY, platformTenants.joinToString(",")),
    )

/** Binds a session principal; [organizationId] non-null makes it an organization principal. */
internal fun signIn(id: String, tenantId: String, organizationId: String? = null) {
    KudosContextHolder.set(
        KudosContext().apply { user = SessionUserPrincipal(id, tenantId, id, organizationId = organizationId) }
    )
}

internal fun signOut() = KudosContextHolder.clear()

/** Sets a (possibly private, possibly inherited) field by reflection. */
internal fun setField(target: Any, name: String, value: Any?) {
    var type: Class<*>? = target.javaClass
    while (type != null) {
        val field = type.declaredFields.firstOrNull { it.name == name }
        if (field != null) {
            field.isAccessible = true
            field.set(target, value)
            return
        }
        type = type.superclass
    }
    error("No field $name on ${target.javaClass}")
}

/** A guard that recognises [PLATFORM_ADMIN] as platform administrator and uses [ownership] when given. */
internal fun accessGuard(ownership: IOrganizationOwnershipService? = null): UserTenantAccessGuard =
    UserTenantAccessGuard().also { guard ->
        setField(guard, "platformAdministratorPolicies", listOf(IPlatformAdministratorPolicy { it == PLATFORM_ADMIN }))
        if (ownership != null) setField(guard, "ownership", ownership)
    }

/** A directory gate over [mode] and [guard], with [policy] wired when given. */
internal fun directoryGate(
    mode: OrganizationMode,
    guard: UserTenantAccessGuard,
    policy: IOrganizationMemberPolicy? = null,
): OrganizationDirectoryGate = OrganizationDirectoryGate(mode, guard).also { gate ->
    if (policy != null) setField(gate, "policy", policy)
}

/** A customer organization root. */
internal fun organizationRoot(id: String, active: Boolean = true): UserOrg = UserOrg {
    this.id = id
    organizationId = id
    nodeKind = UserOrgNodeKind.ORGANIZATION
    tenantId = ""
    parentId = null
    name = id
    this.active = active
}

/** A department of [organizationId] under [parentId] (the root by default). */
internal fun department(id: String, organizationId: String, parentId: String? = organizationId): UserOrg = UserOrg {
    this.id = id
    this.organizationId = organizationId
    nodeKind = UserOrgNodeKind.DEPARTMENT
    tenantId = ""
    this.parentId = parentId
    name = id
    active = true
}

/** A legacy, tenant-owned organization node. */
internal fun legacyOrg(id: String, tenantId: String, parentId: String? = null): UserOrg = UserOrg {
    this.id = id
    this.tenantId = tenantId
    this.parentId = parentId
    name = id
    active = true
}
