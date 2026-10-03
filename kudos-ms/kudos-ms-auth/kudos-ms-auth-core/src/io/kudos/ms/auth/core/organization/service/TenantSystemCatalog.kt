package io.kudos.ms.auth.core.organization.service

import io.kudos.ms.sys.core.system.dao.SysSystemDao
import io.kudos.ms.sys.core.tenant.dao.SysTenantSystemDao
import org.springframework.stereotype.Component

/**
 * The sub-systems a tenant subscribes to: the tenant's entitlement and the upper bound of every role,
 * override and organization administrator inside it. A sub-system counts when it is active and is
 * bound directly or through its parent system.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class TenantSystemCatalog(
    private val tenantSystems: SysTenantSystemDao,
    private val systems: SysSystemDao,
) {

    open fun enabledSubSystemCodes(tenantId: String): Set<String> {
        val bound = tenantSystems.searchSystemCodesByTenantId(tenantId)
        if (bound.isEmpty()) return emptySet()
        val all = systems.allSearch()
        val activeCodes = all.filter { it.active }.mapTo(hashSetOf()) { it.code }
        return all.filter { system ->
            system.active && system.subSystem &&
                (system.code in bound || (system.parentCode in bound && system.parentCode in activeCodes))
        }.mapTo(sortedSetOf()) { it.code }
    }
}
