package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.dao

import io.kudos.ability.data.rdb.ktorm.support.BaseCrudDao
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.po.AuthWebAuthnAttestationPolicy
import io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.table.AuthWebAuthnAttestationPolicies
import org.springframework.stereotype.Repository

@Repository
open class AuthWebAuthnAttestationPolicyDao :
    BaseCrudDao<String, AuthWebAuthnAttestationPolicy, AuthWebAuthnAttestationPolicies>()
