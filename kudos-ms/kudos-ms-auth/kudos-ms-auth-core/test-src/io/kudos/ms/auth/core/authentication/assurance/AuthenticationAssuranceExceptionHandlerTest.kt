package io.kudos.ms.auth.core.authentication.assurance

import io.kudos.ms.auth.common.authentication.vo.AuthenticationAssuranceChallenge
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.setup.MockMvcBuilders
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

internal class AuthenticationAssuranceExceptionHandlerTest {

    @Test
    fun handle_returnsMachineReadableStepUpChallenge() {
        val response = AuthenticationAssuranceExceptionHandler().handle(
            AuthenticationAssuranceRequiredException(
                AuthenticationAssuranceReasonEnum.INSUFFICIENT_ACR,
                DefaultAuthenticationAssurancePolicy.ACR_MFA,
                null,
            )
        )

        assertEquals(AuthenticationAssuranceChallenge.ERROR_CODE, response.code)
        assertEquals("INSUFFICIENT_ACR", response.reason)
        assertEquals(DefaultAuthenticationAssurancePolicy.ACR_MFA, response.requiredAcr)
        assertNull(response.maxAgeSeconds)
        assertEquals(AuthenticationAssuranceChallenge.STEP_UP_ENDPOINT, response.stepUpEndpoint)
    }

    @Test
    fun mvcFailure_returns403AndChallengeContract() {
        val mvc = MockMvcBuilders.standaloneSetup(ChallengeController())
            .setControllerAdvice(AuthenticationAssuranceExceptionHandler())
            .build()

        mvc.get("/assurance-test")
            .andExpect {
                status { isForbidden() }
                jsonPath("$.success") { value(false) }
                jsonPath("$.code") { value(AuthenticationAssuranceChallenge.ERROR_CODE) }
                jsonPath("$.reason") { value("AUTHENTICATION_TOO_OLD") }
                jsonPath("$.requiredAcr") { value(DefaultAuthenticationAssurancePolicy.ACR_MFA) }
                jsonPath("$.maxAgeSeconds") { value(300) }
                jsonPath("$.stepUpEndpoint") { value(AuthenticationAssuranceChallenge.STEP_UP_ENDPOINT) }
            }
    }

    @RestController
    internal class ChallengeController {
        @GetMapping("/assurance-test")
        fun challenge(): Nothing = throw AuthenticationAssuranceRequiredException(
            AuthenticationAssuranceReasonEnum.AUTHENTICATION_TOO_OLD,
            DefaultAuthenticationAssurancePolicy.ACR_MFA,
            300,
        )
    }
}
