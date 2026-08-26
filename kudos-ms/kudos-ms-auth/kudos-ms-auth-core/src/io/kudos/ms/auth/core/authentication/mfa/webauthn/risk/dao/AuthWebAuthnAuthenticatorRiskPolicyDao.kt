package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.po.AuthWebAuthnAuthenticatorRiskPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.table.AuthWebAuthnAuthenticatorRiskPolicies
import org.springframework.stereotype.Repository

@Repository
open class AuthWebAuthnAuthenticatorRiskPolicyDao :
    BaseCrudDao<String, AuthWebAuthnAuthenticatorRiskPolicy, AuthWebAuthnAuthenticatorRiskPolicies>()
