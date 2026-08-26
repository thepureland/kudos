package io.kudos.ms.auth.core.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** Platform-owned external provider template. */
interface AuthProviderTemplate : IDbEntity<String, AuthProviderTemplate> {
    companion object : DbEntityFactory<AuthProviderTemplate>()

    var code: String
    var protocol: String
    var issuer: String?
    var discoveryUri: String?
    var authorizationUri: String?
    var tokenUri: String?
    var userInfoUri: String?
    var jwkSetUri: String?
    var subjectClaim: String
    var defaultScopes: String?
    var adapterType: String?
    var defaultClaimMapping: String?
    var logoUri: String?
    var active: Boolean
    var createTime: LocalDateTime?
    var updateTime: LocalDateTime?
}
