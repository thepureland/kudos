package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One WebAuthn authenticator risk enforcement policy per tenant; id equals tenant id. */
interface AuthWebAuthnAuthenticatorRiskPolicy :
    IDbEntity<String, AuthWebAuthnAuthenticatorRiskPolicy> {
    companion object : DbEntityFactory<AuthWebAuthnAuthenticatorRiskPolicy>()

    var tenantId: String
    var blockedRiskLevels: String?
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var updateUserId: String
    var updateReason: String
    var updateTime: LocalDateTime
}
