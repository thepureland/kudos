package io.kudos.ms.auth.core.authentication.mfa.policy.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One MFA policy per tenant; id equals tenant id. */
interface AuthTenantMfaPolicy : IDbEntity<String, AuthTenantMfaPolicy> {
    companion object : DbEntityFactory<AuthTenantMfaPolicy>()

    var tenantId: String
    var mode: String
    var gracePeriodDays: Int
    var allowedMethods: String
    var recoveryCodesEnabled: Boolean
    var requiredAccountTypeCodes: String?
    var requiredRoleCodes: String?
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var updateUserId: String
    var updateReason: String
    var updateTime: LocalDateTime
}
