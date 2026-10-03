package io.kudos.ms.auth.core.organization.service

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.organization.OrganizationMode
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.org.service.iservice.IOrganizationOwnershipService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

/** Why an organization account may not enter a tenant. */
enum class TenantEntryDenial {
    ORGANIZATION_MODE_DISABLED,
    ACCOUNT_UNAVAILABLE,
    ORGANIZATION_DISABLED,
    TENANT_UNAVAILABLE,
    ORGANIZATION_MISMATCH,
    TENANT_NOT_OPEN,
    SYSTEM_NOT_ENABLED,
    NO_EFFECTIVE_ROLE,
}

/** A tenant an organization account may enter, and the sub-systems it may use there. */
data class TenantEntry(
    val tenantId: String,
    val tenantName: String,
    val organizationId: String,
    val organizationAdmin: Boolean,
    val subSystemCodes: Set<String>,
)

/** Outcome of an entry check: exactly one of [entry] and [denial] is set. */
data class TenantEntryCheck(val entry: TenantEntry?, val denial: TenantEntryDenial?) {
    val allowed: Boolean get() = entry != null
}

/**
 * Who may enter which tenant (G-2, G-4, G-9). An organization account enters tenant t when:
 *
 * 1. the account is active, unfrozen and owned by organization O, and O is active;
 * 2. t is active, owned by O and open;
 * 3. the requested sub-system (if any) is subscribed by t;
 * 4. it is an organization administrator of O, or has at least one effective business role in t
 *    (for that sub-system when one is requested).
 *
 * Management roles do not count: a management-role-only member enters no tenant and works in the
 * organization scope instead. There is no other entry list (G-4): removing every role of a member in a
 * tenant is how one keeps them out.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
open class TenantEntryService(
    private val mode: OrganizationMode,
    private val accounts: UserAccountDao,
    private val tenants: SysTenantDao,
    private val ownership: IOrganizationOwnershipService,
    private val authority: OrganizationAuthority,
    private val roles: EffectiveRoleResolver,
    private val catalog: TenantSystemCatalog,
) {

    open fun check(tenantId: String, userId: String, subSystemCode: String? = null): TenantEntryCheck {
        if (!mode.enabled) return denied(TenantEntryDenial.ORGANIZATION_MODE_DISABLED)
        val account = accounts.get(userId)
        val organizationId = account?.organizationId
        if (account == null || organizationId.isNullOrBlank() || !account.active || isFrozen(account)) {
            return denied(TenantEntryDenial.ACCOUNT_UNAVAILABLE)
        }
        if (ownership.findOrganization(organizationId)?.active != true) return denied(TenantEntryDenial.ORGANIZATION_DISABLED)
        val tenant = tenants.get(tenantId)?.takeIf { it.active } ?: return denied(TenantEntryDenial.TENANT_UNAVAILABLE)
        return checkTenant(tenant, organizationId, userId, subSystemCode)
    }

    /** Tenants of the account's organization it may enter now, in name order. */
    open fun accessibleTenants(userId: String): List<TenantEntry> {
        if (!mode.enabled) return emptyList()
        val account = accounts.get(userId) ?: return emptyList()
        val organizationId = account.organizationId?.takeIf(String::isNotBlank) ?: return emptyList()
        if (!account.active || isFrozen(account) || ownership.findOrganization(organizationId)?.active != true) return emptyList()
        return tenants.search(Criteria(SysTenant::organizationId eq organizationId))
            .filter { it.active }
            .mapNotNull { checkTenant(it, organizationId, userId, null).entry }
            .sortedBy { it.tenantName }
    }

    private fun checkTenant(tenant: SysTenant, organizationId: String, userId: String, subSystemCode: String?): TenantEntryCheck {
        if (tenant.organizationId != organizationId) return denied(TenantEntryDenial.ORGANIZATION_MISMATCH)
        if (!tenant.organizationOpen) return denied(TenantEntryDenial.TENANT_NOT_OPEN)
        val enabled = catalog.enabledSubSystemCodes(tenant.id)
        if (subSystemCode != null && subSystemCode !in enabled) return denied(TenantEntryDenial.SYSTEM_NOT_ENABLED)
        val admin = authority.isOrganizationAdmin(organizationId, userId)
        val systems = if (admin) enabled else {
            val effective = roles.effectiveRoleIds(organizationId, tenant.id, userId)
            roles.organizationRoles(organizationId, effective).values.mapTo(sortedSetOf()) { it.subsysCode }
        }
        val usable = if (subSystemCode == null) systems else systems.filterTo(sortedSetOf()) { it == subSystemCode }
        if (usable.isEmpty()) return denied(TenantEntryDenial.NO_EFFECTIVE_ROLE)
        return TenantEntryCheck(TenantEntry(tenant.id, tenant.name, organizationId, admin, systems), null)
    }

    private fun denied(denial: TenantEntryDenial) = TenantEntryCheck(null, denial)

    private fun isFrozen(account: UserAccount, now: LocalDateTime = LocalDateTime.now()): Boolean =
        !account.freezeType.isNullOrBlank() &&
            (account.freezeStartTime == null || !account.freezeStartTime!!.isAfter(now)) &&
            (account.freezeEndTime == null || account.freezeEndTime!!.isAfter(now))
}
