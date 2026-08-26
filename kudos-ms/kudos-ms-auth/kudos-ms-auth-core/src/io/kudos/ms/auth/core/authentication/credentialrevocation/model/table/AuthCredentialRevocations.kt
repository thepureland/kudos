package io.kudos.ms.auth.core.authentication.credentialrevocation.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.credentialrevocation.model.po.AuthCredentialRevocation
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

object AuthCredentialRevocations :
    StringIdTable<AuthCredentialRevocation>("auth_credential_revocation") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var credentialType = varchar("credential_type").bindTo { it.credentialType }
    var credentialRef = varchar("credential_ref").bindTo { it.credentialRef }
    var credentialFingerprint = varchar("credential_fingerprint").bindTo { it.credentialFingerprint }
    var actorUserId = varchar("actor_user_id").bindTo { it.actorUserId }
    var reason = varchar("reason").bindTo { it.reason }
    var securityEventId = varchar("security_event_id").bindTo { it.securityEventId }
    var leftWithoutFactor = boolean("left_without_factor").bindTo { it.leftWithoutFactor }
    var revokedAt = datetime("revoked_at").bindTo { it.revokedAt }
}
