package io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.po.AuthMfaEnrollmentExemption
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

object AuthMfaEnrollmentExemptions :
    StringIdTable<AuthMfaEnrollmentExemption>("auth_mfa_enrollment_exemption") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var status = varchar("status").bindTo { it.status }
    var reason = varchar("reason").bindTo { it.reason }
    var grantedBy = varchar("granted_by").bindTo { it.grantedBy }
    var grantedAt = datetime("granted_at").bindTo { it.grantedAt }
    var expiresAt = datetime("expires_at").bindTo { it.expiresAt }
    var revokedBy = varchar("revoked_by").bindTo { it.revokedBy }
    var revokeReason = varchar("revoke_reason").bindTo { it.revokeReason }
    var revokedAt = datetime("revoked_at").bindTo { it.revokedAt }
}
