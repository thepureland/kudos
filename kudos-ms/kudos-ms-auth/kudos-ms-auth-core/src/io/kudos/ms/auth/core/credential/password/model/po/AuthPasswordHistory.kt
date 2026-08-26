package io.kudos.ms.auth.core.credential.password.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One retired password hash. Raw passwords are never stored. */
interface AuthPasswordHistory : IDbEntity<String, AuthPasswordHistory> {
    companion object : DbEntityFactory<AuthPasswordHistory>()

    var tenantId: String
    var userId: String
    var purpose: String
    var passwordHash: String
    var recordedAt: LocalDateTime
}
