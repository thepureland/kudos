package io.kudos.ms.auth.core.authentication.mfa.policy.exemption.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One granted exemption; every grant is a new row, so the history stays readable. */
interface AuthMfaEnrollmentExemption : IDbEntity<String, AuthMfaEnrollmentExemption> {
    companion object : DbEntityFactory<AuthMfaEnrollmentExemption>()

    var tenantId: String
    var userId: String
    var status: String
    var reason: String
    var grantedBy: String
    var grantedAt: LocalDateTime
    var expiresAt: LocalDateTime
    var revokedBy: String?
    var revokeReason: String?
    var revokedAt: LocalDateTime?
}
