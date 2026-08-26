package io.kudos.ms.auth.core.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object AuthProviderTemplates : StringIdTable<AuthProviderTemplate>("auth_provider_template") {
    var code = varchar("code").bindTo { it.code }
    var protocol = varchar("protocol").bindTo { it.protocol }
    var issuer = varchar("issuer").bindTo { it.issuer }
    var discoveryUri = varchar("discovery_uri").bindTo { it.discoveryUri }
    var authorizationUri = varchar("authorization_uri").bindTo { it.authorizationUri }
    var tokenUri = varchar("token_uri").bindTo { it.tokenUri }
    var userInfoUri = varchar("user_info_uri").bindTo { it.userInfoUri }
    var jwkSetUri = varchar("jwk_set_uri").bindTo { it.jwkSetUri }
    var subjectClaim = varchar("subject_claim").bindTo { it.subjectClaim }
    var defaultScopes = varchar("default_scopes").bindTo { it.defaultScopes }
    var adapterType = varchar("adapter_type").bindTo { it.adapterType }
    var defaultClaimMapping = text("default_claim_mapping").bindTo { it.defaultClaimMapping }
    var logoUri = varchar("logo_uri").bindTo { it.logoUri }
    var active = boolean("active").bindTo { it.active }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
