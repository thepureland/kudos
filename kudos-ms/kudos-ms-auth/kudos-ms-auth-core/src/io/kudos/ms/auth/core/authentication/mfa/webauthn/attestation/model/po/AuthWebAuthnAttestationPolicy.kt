package io.kudos.ms.auth.core.authentication.mfa.webauthn.attestation.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One WebAuthn attestation admission policy per tenant; id equals tenant id. */
interface AuthWebAuthnAttestationPolicy : IDbEntity<String, AuthWebAuthnAttestationPolicy> {
    companion object : DbEntityFactory<AuthWebAuthnAttestationPolicy>()

    var tenantId: String
    var aaguidMode: String
    var aaguids: String?
    var allowedAttestationFormats: String?
    var requireTrustedAttestation: Boolean
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var updateUserId: String
    var updateReason: String
    var updateTime: LocalDateTime
}
