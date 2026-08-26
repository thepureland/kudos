package io.kudos.ms.auth.core.token.refresh.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.token.refresh.model.po.AuthRefreshToken
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object AuthRefreshTokens : StringIdTable<AuthRefreshToken>("auth_refresh_token") {
    var sessionId = varchar("session_id").bindTo { it.sessionId }
    var familyId = varchar("family_id").bindTo { it.familyId }
    var parentId = varchar("parent_id").bindTo { it.parentId }
    var replacedById = varchar("replaced_by_id").bindTo { it.replacedById }
    var tokenHash = varchar("token_hash").bindTo { it.tokenHash }
    var tokenEpoch = long("token_epoch").bindTo { it.tokenEpoch }
    var issuedAt = datetime("issued_at").bindTo { it.issuedAt }
    var expiresAt = datetime("expires_at").bindTo { it.expiresAt }
    var consumedAt = datetime("consumed_at").bindTo { it.consumedAt }
    var revokedAt = datetime("revoked_at").bindTo { it.revokedAt }
    var revokeReason = varchar("revoke_reason").bindTo { it.revokeReason }
}
