package io.kudos.ms.auth.core.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Tenant-owned instance of an external provider template. */
interface AuthIdentityProvider : IDbEntity<String, AuthIdentityProvider> {
    companion object : DbEntityFactory<AuthIdentityProvider>()

    var tenantId: String
    var templateId: String
    var code: String
    var displayName: String
    var issuer: String?
    var clientId: String
    var clientSecretRef: String?
    var scopes: String?
    var customConfig: String?
    var jitPolicy: String
    var linkPolicy: String
    var active: Boolean
    var createUserId: String?
    var createReason: String?
    var createTime: LocalDateTime?
    var updateUserId: String?
    var updateReason: String?
    var updateTime: LocalDateTime?
}
