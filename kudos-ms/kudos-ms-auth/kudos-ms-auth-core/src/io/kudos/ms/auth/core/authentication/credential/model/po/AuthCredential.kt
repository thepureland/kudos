package io.kudos.ms.auth.core.authentication.credential.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One stored authentication secret; the encoded value never leaves core. */
interface AuthCredential : IDbEntity<String, AuthCredential> {
    companion object : DbEntityFactory<AuthCredential>()

    var tenantId: String
    var userId: String
    var type: String
    var secretHashOrRef: String
    var status: String
    var version: Long
    var enrolledAt: LocalDateTime
    var expiresAt: LocalDateTime?
    var lastUsedAt: LocalDateTime?
    var metadata: String?
    var revokedAt: LocalDateTime?
    var revokeReason: String?
    var createTime: LocalDateTime
    var updateTime: LocalDateTime
}
