package io.kudos.ms.auth.core.authentication.mfa.recovery.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.mfa.recovery.model.po.AuthRecoveryCode
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

object AuthRecoveryCodes : StringIdTable<AuthRecoveryCode>("auth_recovery_code") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var setId = varchar("set_id").bindTo { it.setId }
    var codeHash = varchar("code_hash").bindTo { it.codeHash }
    var createdAt = datetime("created_at").bindTo { it.createdAt }
    var consumedAt = datetime("consumed_at").bindTo { it.consumedAt }
    var revokedAt = datetime("revoked_at").bindTo { it.revokedAt }
}
