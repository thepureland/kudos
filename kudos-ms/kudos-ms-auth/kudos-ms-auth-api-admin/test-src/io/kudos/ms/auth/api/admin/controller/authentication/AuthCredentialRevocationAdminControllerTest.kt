package io.kudos.ms.auth.api.admin.controller.authentication

import io.kudos.ability.security.enforcement.annotation.RequiresPermission
import io.kudos.context.core.KudosContext
import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.auth.common.authentication.credentialrevocation.vo.AuthCredentialRevocationAdminRequest
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationCommand
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationException
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialRevocationResult
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.AuthCredentialTypeEnum
import io.kudos.ms.auth.core.authentication.credentialrevocation.service.iservice.IAuthCredentialRevocationService
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import org.mockito.ArgumentCaptor
import org.mockito.Mockito.mock
import org.mockito.Mockito.verify
import org.mockito.Mockito.verifyNoInteractions
import org.mockito.Mockito.`when`
import org.springframework.web.server.ResponseStatusException
import java.time.LocalDateTime
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

internal class AuthCredentialRevocationAdminControllerTest {
    private val service = mock(IAuthCredentialRevocationService::class.java)
    private val userAccountService = mock(IUserAccountService::class.java)
    private val controller = AuthCredentialRevocationAdminController(service, userAccountService)

    @BeforeTest
    fun bindOperator() {
        KudosContextHolder.set(
            KudosContext().apply {
                user = SessionUserPrincipal("admin-1", "tenant-1", "administrator")
            }
        )
        `when`(userAccountService.getUserRecord("user-1"))
            .thenReturn(UserAccountRow(id = "user-1", tenantId = "tenant-1"))
    }

    @AfterTest
    fun clearOperator() = KudosContextHolder.clear()

    @Test
    fun revocationPinsTheTenantAndTheOperatorToTheAdministratorSession() {
        `when`(service.revoke(anyCommand())).thenReturn(result())

        val revoked = controller.revoke(request())

        assertEquals("user-1", revoked.userId)
        val command = ArgumentCaptor.forClass(AuthCredentialRevocationCommand::class.java)
        verify(service).revoke(command.capture() ?: anyCommand())
        assertEquals("tenant-1", command.value.tenantId)
        assertEquals("admin-1", command.value.actorUserId)
        assertEquals(AuthCredentialTypeEnum.WEBAUTHN, command.value.credentialType)
        assertEquals("row-1", command.value.credentialRef)
        assertEquals("event-1", command.value.securityEventId)
    }

    @Test
    fun aTargetFromAnotherTenantIsReportedAsAbsent() {
        `when`(userAccountService.getUserRecord("outsider"))
            .thenReturn(UserAccountRow(id = "outsider", tenantId = "tenant-2"))

        val crossTenant = assertFailsWith<ResponseStatusException> {
            controller.revoke(request(userId = "outsider"))
        }

        assertEquals(404, crossTenant.statusCode.value())
        verifyNoInteractions(service)
    }

    @Test
    fun endpointsUseDedicatedPermissionsAndRequireASession() {
        val viewPermission = AuthCredentialRevocationAdminController::class.java
            .getDeclaredMethod("list", String::class.java, Int::class.javaPrimitiveType)
            .getAnnotation(RequiresPermission::class.java)
        val revokePermission = AuthCredentialRevocationAdminController::class.java
            .getDeclaredMethod("revoke", AuthCredentialRevocationAdminRequest::class.java)
            .getAnnotation(RequiresPermission::class.java)
        KudosContextHolder.clear()

        val unauthorized = assertFailsWith<ResponseStatusException> { controller.list() }

        assertEquals(401, unauthorized.statusCode.value())
        // Reading the revocation history is not the same right as taking somebody's credential away.
        assertEquals("auth:credential-revocation:view", viewPermission.value)
        assertEquals("auth:credential-revocation:revoke", revokePermission.value)
        verifyNoInteractions(service)
    }

    @Test
    fun domainRefusalsKeepTheirOwnHttpSemantics() {
        `when`(service.revoke(anyCommand())).thenThrow(
            AuthCredentialRevocationException("AUTH_CREDENTIAL_REVOCATION_CREDENTIAL_NOT_FOUND"),
            AuthCredentialRevocationException("AUTH_CREDENTIAL_REVOCATION_ALREADY_REVOKED"),
            AuthCredentialRevocationException("AUTH_CREDENTIAL_REVOCATION_REASON_INVALID"),
        )

        val missing = assertFailsWith<ResponseStatusException> { controller.revoke(request()) }
        val alreadyGone = assertFailsWith<ResponseStatusException> { controller.revoke(request()) }
        val malformed = assertFailsWith<ResponseStatusException> { controller.revoke(request()) }
        val badType = assertFailsWith<ResponseStatusException> {
            controller.revoke(request(credentialType = "SMART_CARD"))
        }

        assertEquals(404, missing.statusCode.value())
        assertEquals(409, alreadyGone.statusCode.value())
        assertEquals(400, malformed.statusCode.value())
        assertEquals(400, badType.statusCode.value())
    }

    private fun request(
        userId: String = " user-1 ",
        credentialType: String = "webauthn",
    ) = AuthCredentialRevocationAdminRequest(
        userId = userId,
        credentialType = credentialType,
        credentialRef = " row-1 ",
        reason = "authenticator reported stolen",
        securityEventId = "event-1",
    )

    private fun anyCommand() = AuthCredentialRevocationCommand(
        tenantId = "tenant-1",
        userId = "user-1",
        credentialType = AuthCredentialTypeEnum.WEBAUTHN,
        credentialRef = "row-1",
        actorUserId = "admin-1",
        reason = "authenticator reported stolen",
        securityEventId = "event-1",
    )

    private fun result() = AuthCredentialRevocationResult(
        id = "revocation-1",
        tenantId = "tenant-1",
        userId = "user-1",
        credentialType = AuthCredentialTypeEnum.WEBAUTHN,
        credentialRef = "row-1",
        credentialFingerprint = "fingerprint",
        actorUserId = "admin-1",
        reason = "authenticator reported stolen",
        securityEventId = "event-1",
        leftWithoutFactor = false,
        enrollmentBlocked = false,
        revokedAt = LocalDateTime.parse("2026-08-26T10:00:00"),
    )
}
