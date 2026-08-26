package io.kudos.ms.auth.core.authentication.credential.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.credential.model.po.AuthCredential
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object AuthCredentials : StringIdTable<AuthCredential>("auth_credential") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var type = varchar("type").bindTo { it.type }
    var secretHashOrRef = varchar("secret_hash_or_ref").bindTo { it.secretHashOrRef }
    var status = varchar("status").bindTo { it.status }
    var version = long("version").bindTo { it.version }
    var enrolledAt = datetime("enrolled_at").bindTo { it.enrolledAt }
    var expiresAt = datetime("expires_at").bindTo { it.expiresAt }
    var lastUsedAt = datetime("last_used_at").bindTo { it.lastUsedAt }
    var metadata = varchar("metadata").bindTo { it.metadata }
    var revokedAt = datetime("revoked_at").bindTo { it.revokedAt }
    var revokeReason = varchar("revoke_reason").bindTo { it.revokeReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
