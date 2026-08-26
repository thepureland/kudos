package io.kudos.ms.auth.core.authentication.mfa

import org.springframework.http.HttpStatus
import kotlin.test.Test
import kotlin.test.assertEquals

internal class TotpEnrollmentExceptionHandlerTest {

    private val handler = TotpEnrollmentExceptionHandler()

    @Test
    fun invalidCodeUsesStableBadRequestContract() {
        val response = handler.handle(
            TotpEnrollmentException(TotpEnrollmentErrorCodeEnum.INVALID_TOTP_CODE, "invalid"),
        )

        assertEquals(HttpStatus.BAD_REQUEST, response.statusCode)
        assertEquals("INVALID_TOTP_CODE", response.body?.code)
        assertEquals(false, response.body?.success)
    }

    @Test
    fun exhaustedAttemptsAreRateLimited() {
        val response = handler.handle(
            TotpEnrollmentException(TotpEnrollmentErrorCodeEnum.TOTP_ATTEMPTS_EXCEEDED, "exhausted"),
        )

        assertEquals(HttpStatus.TOO_MANY_REQUESTS, response.statusCode)
    }
}
