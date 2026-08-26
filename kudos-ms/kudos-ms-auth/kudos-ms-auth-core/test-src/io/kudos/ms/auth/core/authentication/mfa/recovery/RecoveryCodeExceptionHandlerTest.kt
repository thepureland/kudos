package io.kudos.ms.auth.core.authentication.mfa.recovery

import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals

internal class RecoveryCodeExceptionHandlerTest {

    private val handler = RecoveryCodeExceptionHandler()

    @Test
    fun disabledMfaUsesStableConflictResponse() {
        val response = handler.handle(
            RecoveryCodeException(RecoveryCodeErrorCodeEnum.MFA_NOT_ENABLED, "disabled")
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("MFA_NOT_ENABLED", response.body?.code)
        assertEquals(false, response.body?.success)
    }

    @Test
    fun tenantPolicyDisableUsesStableConflictResponse() {
        val response = handler.handle(
            RecoveryCodeException(RecoveryCodeErrorCodeEnum.RECOVERY_CODES_DISABLED, "disabled by policy")
        )

        assertEquals(HttpStatus.CONFLICT, response.statusCode)
        assertEquals("RECOVERY_CODES_DISABLED", response.body?.code)
    }
}
