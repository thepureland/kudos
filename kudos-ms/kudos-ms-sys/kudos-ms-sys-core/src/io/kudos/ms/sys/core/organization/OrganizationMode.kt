package io.kudos.ms.sys.core.organization

import io.kudos.ms.sys.core.organization.init.OrganizationModeProperties
import org.springframework.boot.context.properties.bind.Bindable
import org.springframework.boot.context.properties.bind.Binder
import org.springframework.core.env.Environment
import org.springframework.stereotype.Component

/**
 * Single answer to "is this deployment in organization mode, and which tenants are the platform's".
 *
 * Platform tenants are read from the same key the authorization policy uses
 * (`kudos.ms.auth.authz.platform-admin-tenant-ids`), so the two can never disagree. In organization
 * mode they keep the legacy tenant-owned identity model; every other tenant must belong to an
 * organization before anybody can enter it.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class OrganizationMode(
    private val properties: OrganizationModeProperties,
    environment: Environment,
) {

    private val platformTenantIds: Set<String> = Binder.get(environment)
        .bind(PLATFORM_TENANT_IDS_KEY, Bindable.setOf(String::class.java))
        .orElse(null) ?: emptySet()

    /** Whether organization mode is on. */
    open val enabled: Boolean get() = properties.enabled

    /** Whether [tenantId] is a platform tenant (legacy identity model in either mode). */
    open fun isPlatformTenant(tenantId: String?): Boolean = tenantId != null && tenantId in platformTenantIds

    /** The configured platform tenant ids. */
    open fun platformTenantIds(): Set<String> = platformTenantIds

    /** Fails with [code] unless organization mode is on. */
    open fun requireEnabled(code: String = "ORGANIZATION_MODE_DISABLED") {
        if (!enabled) throw IllegalStateException(code)
    }

    companion object {
        const val PLATFORM_TENANT_IDS_KEY = "kudos.ms.auth.authz.platform-admin-tenant-ids"
    }
}
