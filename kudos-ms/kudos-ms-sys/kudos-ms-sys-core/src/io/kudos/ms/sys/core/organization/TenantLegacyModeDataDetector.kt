package io.kudos.ms.sys.core.organization

import io.kudos.ms.sys.core.organization.spi.ILegacyModeDataDetector
import io.kudos.ms.sys.core.tenant.dao.SysTenantDao
import org.springframework.stereotype.Component

/**
 * Legacy tenants: any tenant that is neither a platform tenant nor owned by an organization. Built-in
 * seed tenants are not customer data and are not counted; legacy accounts inside them are counted by
 * the user module.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class TenantLegacyModeDataDetector(private val tenantDao: SysTenantDao) : ILegacyModeDataDetector {

    override fun countLegacyData(platformTenantIds: Set<String>): Map<String, Int> =
        mapOf("tenants" to tenantDao.allSearch().count { it.organizationId == null && !it.builtIn && it.id !in platformTenantIds })
}
