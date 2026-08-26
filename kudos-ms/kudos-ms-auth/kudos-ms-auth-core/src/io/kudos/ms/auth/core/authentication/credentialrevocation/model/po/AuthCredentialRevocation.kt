package io.kudos.ms.auth.core.authentication.credentialrevocation.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Append-only record of one administrator-initiated credential revocation. */
interface AuthCredentialRevocation : IDbEntity<String, AuthCredentialRevocation> {
    companion object : DbEntityFactory<AuthCredentialRevocation>()

    var tenantId: String
    var userId: String
    var credentialType: String
    var credentialRef: String?
    var credentialFingerprint: String?
    var actorUserId: String
    var reason: String
    var securityEventId: String?
    var leftWithoutFactor: Boolean
    var revokedAt: LocalDateTime
}
