package io.kudos.ms.auth.core.credential.password.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.credential.password.model.po.AuthPasswordHistory
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

object AuthPasswordHistories : StringIdTable<AuthPasswordHistory>("auth_password_history") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var purpose = varchar("purpose").bindTo { it.purpose }
    var passwordHash = varchar("password_hash").bindTo { it.passwordHash }
    var recordedAt = datetime("recorded_at").bindTo { it.recordedAt }
}
