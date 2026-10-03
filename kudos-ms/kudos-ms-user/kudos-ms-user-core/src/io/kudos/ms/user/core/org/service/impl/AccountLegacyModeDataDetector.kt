package io.kudos.ms.user.core.org.service.impl

import io.kudos.ms.sys.core.organization.spi.ILegacyModeDataDetector
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.org.dao.UserOrgDao
import org.springframework.stereotype.Component

/**
 * Legacy accounts and departments: tenant-owned rows outside the platform tenants.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class AccountLegacyModeDataDetector(
    private val accountDao: UserAccountDao,
    private val orgDao: UserOrgDao,
) : ILegacyModeDataDetector {

    override fun countLegacyData(platformTenantIds: Set<String>): Map<String, Int> = mapOf(
        "accounts" to accountDao.allSearch().count { it.organizationId == null && it.tenantId !in platformTenantIds },
        "departments" to orgDao.allSearch().count { it.organizationId == null && it.tenantId !in platformTenantIds },
    )
}
