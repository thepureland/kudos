package io.kudos.ms.auth.core.organization.service

import io.kudos.base.query.Criteria
import io.kudos.base.query.eq
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import io.kudos.ms.sys.core.tenant.model.po.SysTenant
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional

/** A tenant of the organization, as the console lists it. */
data class OrganizationTenantRow(val tenantId: String, val name: String, val active: Boolean, val open: Boolean, val subSystemCodes: List<String>)

/** The organization at a glance: its revision, the caller's rank, and its tenants. */
data class OrganizationOverview(
    val organizationId: String,
    val revision: Long,
    val callerRank: String,
    /** Tenants whose overrides the caller may manage; null means all. */
    val manageableTenantIds: Set<String>?,
    val tenants: List<OrganizationTenantRow>,
)

/**
 * Read model of one organization for the console.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Service
@Transactional(readOnly = true)
open class OrganizationOverviewService(
    private val tenants: SysTenantDao,
    private val catalog: TenantSystemCatalog,
    private val policy: OrganizationAdministrationPolicy,
    private val recorder: OrganizationChangeRecorder,
) {

    open fun overview(organizationId: String): OrganizationOverview {
        policy.assertCanReadOrganization(organizationId)
        return OrganizationOverview(
            organizationId = organizationId,
            revision = recorder.current(organizationId),
            callerRank = policy.actorRank(organizationId).name,
            manageableTenantIds = policy.manageableTenantIds(organizationId),
            tenants = tenants.search(Criteria(SysTenant::organizationId eq organizationId)).sortedBy { it.name }.map {
                OrganizationTenantRow(it.id, it.name, it.active, it.organizationOpen, catalog.enabledSubSystemCodes(it.id).toList())
            },
        )
    }
}
