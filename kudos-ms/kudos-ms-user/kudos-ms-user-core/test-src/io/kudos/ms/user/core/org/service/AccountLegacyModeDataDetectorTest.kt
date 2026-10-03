package io.kudos.ms.user.core.org.service

import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.model.po.UserAccount
import io.kudos.ms.user.core.org.dao.UserOrgDao
import io.kudos.ms.user.core.org.service.impl.AccountLegacyModeDataDetector
import io.kudos.ms.user.core.security.PLATFORM_TENANT
import io.kudos.ms.user.core.security.department
import io.kudos.ms.user.core.security.legacyOrg
import io.kudos.ms.user.core.security.organizationRoot
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.Test
import kotlin.test.assertEquals

/** Pure test of [AccountLegacyModeDataDetector]: only tenant-owned rows outside platform tenants count. */
internal class AccountLegacyModeDataDetectorTest {

    private val accountDao = mock(UserAccountDao::class.java)
    private val orgDao = mock(UserOrgDao::class.java)
    private val detector = AccountLegacyModeDataDetector(accountDao, orgDao)

    private fun account(id: String, tenantId: String, organizationId: String? = null) = UserAccount {
        this.id = id
        this.tenantId = tenantId
        this.organizationId = organizationId
        username = id
    }

    @Test
    fun emptyDatabaseHasNoLegacyData() {
        whenCalled(accountDao.allSearch()).thenReturn(emptyList())
        whenCalled(orgDao.allSearch()).thenReturn(emptyList())
        assertEquals(mapOf("accounts" to 0, "departments" to 0), detector.countLegacyData(setOf(PLATFORM_TENANT)))
    }

    @Test
    fun countsOnlyTenantOwnedRowsOutsidePlatformTenants() {
        whenCalled(accountDao.allSearch()).thenReturn(
            listOf(
                account("root", PLATFORM_TENANT),
                account("legacy-1", "tenant-a"),
                account("legacy-2", "tenant-b"),
                account("member", "", organizationId = "org-1"),
            )
        )
        whenCalled(orgDao.allSearch()).thenReturn(
            listOf(
                legacyOrg("platform-dept", PLATFORM_TENANT),
                legacyOrg("legacy-dept", "tenant-a"),
                organizationRoot("org-1"),
                department("dept-1", "org-1"),
            )
        )
        assertEquals(mapOf("accounts" to 2, "departments" to 1), detector.countLegacyData(setOf(PLATFORM_TENANT)))
    }

    @Test
    fun withoutPlatformTenantsEveryTenantOwnedRowCounts() {
        whenCalled(accountDao.allSearch()).thenReturn(listOf(account("root", PLATFORM_TENANT), account("member", "", "org-1")))
        whenCalled(orgDao.allSearch()).thenReturn(listOf(legacyOrg("platform-dept", PLATFORM_TENANT)))
        assertEquals(mapOf("accounts" to 1, "departments" to 1), detector.countLegacyData(emptySet()))
    }
}
