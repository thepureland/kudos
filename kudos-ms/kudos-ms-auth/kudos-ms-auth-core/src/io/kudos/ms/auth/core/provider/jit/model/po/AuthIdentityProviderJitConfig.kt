package io.kudos.ms.auth.core.provider.jit.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Tenant-owned JIT defaults for one external Provider instance; id equals the Provider id. */
interface AuthIdentityProviderJitConfig : IDbEntity<String, AuthIdentityProviderJitConfig> {
    companion object : DbEntityFactory<AuthIdentityProviderJitConfig>()

    var tenantId: String
    var usernameStrategy: String
    var requireVerifiedEmail: Boolean
    var allowedEmailDomains: String?
    var defaultOrgId: String?
    var defaultSupervisorId: String?
    var accountTypeDictCode: String?
    var accountStatusDictCode: String?
    var defaultLocale: String?
    var defaultTimezone: String?
    var defaultCurrency: String?
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var updateUserId: String
    var updateReason: String
    var updateTime: LocalDateTime
}
