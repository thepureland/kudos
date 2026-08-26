package io.kudos.ms.auth.core.authentication.mfa.policy

import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.service.impl.MfaEnrollmentQuery
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`
import kotlin.test.Test
import kotlin.test.assertEquals

internal class MfaEnrollmentQueryTest {

    @Test
    fun reportsEveryActuallyEnrolledMethod() {
        val totp = mock(ITotpEnrollmentService::class.java)
        val webAuthn = mock(IWebAuthnCredentialService::class.java)
        `when`(totp.isEnabled("u-1", "t-1")).thenReturn(true)
        `when`(webAuthn.isEnrolled("u-1", "t-1")).thenReturn(true)

        val methods = MfaEnrollmentQuery(totp, webAuthn).enrolledMethods("u-1", "t-1")

        assertEquals(setOf(MfaMethodEnum.TOTP, MfaMethodEnum.WEBAUTHN), methods)
    }
}
