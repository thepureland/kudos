package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.service.iservice

import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.EffectiveWebAuthnAuthenticatorRiskPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskEnforcementCommand
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.WebAuthnAuthenticatorRiskPolicySaveCommand

interface IWebAuthnAuthenticatorRiskPolicyService {
    fun getEffective(tenantId: String): EffectiveWebAuthnAuthenticatorRiskPolicy

    fun isRiskEvaluationAvailable(): Boolean

    fun save(command: WebAuthnAuthenticatorRiskPolicySaveCommand): EffectiveWebAuthnAuthenticatorRiskPolicy

    fun enforce(command: WebAuthnAuthenticatorRiskEnforcementCommand)
}
