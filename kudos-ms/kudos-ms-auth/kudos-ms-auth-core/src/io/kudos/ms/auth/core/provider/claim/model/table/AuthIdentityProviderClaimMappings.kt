package io.kudos.ms.auth.core.provider.claim.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.provider.claim.model.po.AuthIdentityProviderClaimMapping
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

object AuthIdentityProviderClaimMappings :
    StringIdTable<AuthIdentityProviderClaimMapping>("auth_identity_provider_claim_mapping") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var subjectClaims = varchar("subject_claims").bindTo { it.subjectClaims }
    var usernameClaims = varchar("username_claims").bindTo { it.usernameClaims }
    var displayNameClaims = varchar("display_name_claims").bindTo { it.displayNameClaims }
    var emailClaims = varchar("email_claims").bindTo { it.emailClaims }
    var emailVerifiedClaims = varchar("email_verified_claims").bindTo { it.emailVerifiedClaims }
    var phoneClaims = varchar("phone_claims").bindTo { it.phoneClaims }
    var phoneVerifiedClaims = varchar("phone_verified_claims").bindTo { it.phoneVerifiedClaims }
    var avatarClaims = varchar("avatar_claims").bindTo { it.avatarClaims }
    var localeClaims = varchar("locale_claims").bindTo { it.localeClaims }
    var unionIdClaims = varchar("union_id_claims").bindTo { it.unionIdClaims }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
