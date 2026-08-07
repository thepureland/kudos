package io.kudos.ms.auth.core.policy

import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.core.platform.authz.PlatformAdministratorPolicy
import io.kudos.ms.auth.core.principal.PrincipalDirectoryRegistry
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * @author K
 * @author AI: Codex
 */
internal class TenantAdministrationGuardTest {

    private val platformPolicy = mock(PlatformAdministratorPolicy::class.java)
    private val guard = TenantAdministrationGuard().apply {
        inject("platformAdministratorPolicy", platformPolicy)
        inject("principalDirectoryRegistry", mock(PrincipalDirectoryRegistry::class.java))
    }

    @AfterTest
    fun cleanup() = KudosContextHolder.clear()

    @Test
    fun tenantAdministratorCannotManageAnotherTenant() {
        login("operator", "tenant-a")
        assertFailsWith<IllegalArgumentException> { guard.assertCanManage("tenant-b") }
    }

    @Test
    fun platformAdministratorMayCrossTenantBoundary() {
        login("platform-user", "platform")
        whenCalled(platformPolicy.isPlatformAdministrator("platform-user")).thenReturn(true)
        guard.assertCanManage("tenant-b")
    }

    @Test
    fun ordinaryTenantCannotMintReservedRoleCode() {
        login("operator", "tenant-a")
        whenCalled(platformPolicy.isReservedRoleCode("PLATFORM_ADMIN")).thenReturn(true)
        assertFailsWith<IllegalArgumentException> {
            guard.assertRoleCodeManageable("PLATFORM_ADMIN", "tenant-a")
        }
    }

    private fun login(id: String, tenantId: String) {
        val context = KudosContext()
        context.user = SessionUserPrincipal(id, tenantId, id)
        KudosContextHolder.set(context)
    }

    private fun Any.inject(field: String, value: Any) {
        val f = this::class.java.getDeclaredField(field)
        f.isAccessible = true
        f.set(this, value)
    }
}
