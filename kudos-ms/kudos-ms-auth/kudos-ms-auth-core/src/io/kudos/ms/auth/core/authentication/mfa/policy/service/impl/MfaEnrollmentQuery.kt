package io.kudos.ms.auth.core.authentication.mfa.policy.service.impl

import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.core.authentication.mfa.policy.service.iservice.IMfaEnrollmentQuery
import io.kudos.ms.auth.core.authentication.mfa.service.iservice.ITotpEnrollmentService
import io.kudos.ms.auth.core.authentication.mfa.webauthn.service.iservice.IWebAuthnCredentialService
import org.springframework.stereotype.Service

@Service
open class MfaEnrollmentQuery(
    private val totpEnrollmentService: ITotpEnrollmentService,
    private val webAuthnCredentialService: IWebAuthnCredentialService,
) : IMfaEnrollmentQuery {

    override fun enrolledMethods(userId: String, tenantId: String): Set<MfaMethodEnum> = buildSet {
        if (totpEnrollmentService.isEnabled(userId, tenantId)) add(MfaMethodEnum.TOTP)
        if (webAuthnCredentialService.isEnrolled(userId, tenantId)) add(MfaMethodEnum.WEBAUTHN)
    }
}
