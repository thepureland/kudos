package io.kudos.ms.auth.core.provider.invitation.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.provider.invitation.model.po.AuthExternalIdentityInvitation
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

object AuthExternalIdentityInvitations :
    StringIdTable<AuthExternalIdentityInvitation>("auth_external_identity_invitation") {

    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var identityProviderId = varchar("identity_provider_id").bindTo { it.identityProviderId }
    var tokenHash = varchar("token_hash").bindTo { it.tokenHash }
    var expectedEmailHash = varchar("expected_email_hash").bindTo { it.expectedEmailHash }
    var maxUses = int("max_uses").bindTo { it.maxUses }
    var usedCount = int("used_count").bindTo { it.usedCount }
    var expiresAt = datetime("expires_at").bindTo { it.expiresAt }
    var active = boolean("active").bindTo { it.active }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var revokeUserId = varchar("revoke_user_id").bindTo { it.revokeUserId }
    var revokeReason = varchar("revoke_reason").bindTo { it.revokeReason }
    var revokeTime = datetime("revoke_time").bindTo { it.revokeTime }
    var lastUsedTime = datetime("last_used_time").bindTo { it.lastUsedTime }
    var consumedSubjectHash = varchar("consumed_subject_hash").bindTo { it.consumedSubjectHash }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
