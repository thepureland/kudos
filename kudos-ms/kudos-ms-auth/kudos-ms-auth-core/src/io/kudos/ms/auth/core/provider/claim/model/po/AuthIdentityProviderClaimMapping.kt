package io.kudos.ms.auth.core.provider.claim.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Ordered, Provider-scoped claim paths; id equals the Provider id. */
interface AuthIdentityProviderClaimMapping : IDbEntity<String, AuthIdentityProviderClaimMapping> {
    companion object : DbEntityFactory<AuthIdentityProviderClaimMapping>()

    var tenantId: String
    var subjectClaims: String
    var usernameClaims: String?
    var displayNameClaims: String?
    var emailClaims: String?
    var emailVerifiedClaims: String?
    var phoneClaims: String?
    var phoneVerifiedClaims: String?
    var avatarClaims: String?
    var localeClaims: String?
    var unionIdClaims: String?
    var createUserId: String
    var createReason: String
    var createTime: LocalDateTime
    var updateUserId: String
    var updateReason: String
    var updateTime: LocalDateTime
}
