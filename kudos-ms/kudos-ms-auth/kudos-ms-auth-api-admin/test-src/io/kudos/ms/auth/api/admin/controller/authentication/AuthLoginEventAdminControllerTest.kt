package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.core.authentication.loginevent.service.iservice.IAuthLoginEventService
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthLoginEventAdminControllerTest {
    private val service = mock(IAuthLoginEventService::class.java)
    private val userAccountService = mock(IUserAccountService::class.java)
    private val controller = AuthLoginEventAdminController(service, userAccountService)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
            }
        )
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun theQueryIsPinnedToTheAdministratorsOwnTenant() {
        `when`(userAccountService.getUserRecord("user-1"))
            .thenReturn(UserAccountRow(id = "user-1", tenantId = "tenant-1"))
        `when`(service.listRecent("tenant-1", "user-1", "alice", true, 50)).thenReturn(emptyList())

        assertEquals(emptyList(), controller.list(" user-1 ", " alice ", true, 50))

        // The plain name reaches the service, which hashes it — the controller never sees or logs a digest.
        verify(service).listRecent("tenant-1", "user-1", "alice", true, 50)
    }

    @Test
    fun aSubjectFromAnotherTenantIsReportedAsAbsent() {
        `when`(userAccountService.getUserRecord("outsider"))
            .thenReturn(UserAccountRow(id = "outsider", tenantId = "tenant-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> { controller.list(userId = "outsider") }

        assertEquals(404, crossTenant.statusCode.value())
        verifyNoInteractions(service)
    }

    @Test
    fun theEndpointUsesItsOwnPermissionAndRequiresASession() {
        val permission = AuthLoginEventAdminController::class.java
            .getDeclaredMethod(
                "list",
                String::class.java,
                String::class.java,
                java.lang.Boolean::class.java,
                Int::class.javaPrimitiveType,
            )
            .getAnnotation(RequiresPermission::class.java)
        KudosContextHolder.clear()

        val unauthorized = assertFailsWith<ResponseStatusException> { controller.list() }

        assertEquals(401, unauthorized.statusCode.value())
        assertEquals("auth:login-event:view", permission.value)
        verifyNoInteractions(service)
    }

    @Test
    fun boundedQueriesOnly() {
        val tooMany = assertFailsWith<ResponseStatusException> { controller.list(limit = 501) }
        val blankUser = assertFailsWith<ResponseStatusException> { controller.list(userId = " ") }

        assertEquals(400, tooMany.statusCode.value())
        assertEquals(400, blankUser.statusCode.value())
    }
}
