package io.kudos.ms.auth.core.authentication.mfa.recovery.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One hash-only, single-use member of a recovery-code set. */
interface AuthRecoveryCode : IDbEntity<String, AuthRecoveryCode> {
    companion object : DbEntityFactory<AuthRecoveryCode>()

    var tenantId: String
    var userId: String
    var setId: String
    var codeHash: String
    var createdAt: LocalDateTime
    var consumedAt: LocalDateTime?
    var revokedAt: LocalDateTime?
}
