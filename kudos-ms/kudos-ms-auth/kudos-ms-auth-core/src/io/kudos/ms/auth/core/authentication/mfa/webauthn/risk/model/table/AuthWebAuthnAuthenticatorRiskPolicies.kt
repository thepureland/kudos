package io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.mfa.webauthn.risk.model.po.AuthWebAuthnAuthenticatorRiskPolicy
import org.ktorm.schema.datetime
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object AuthWebAuthnAuthenticatorRiskPolicies :
    StringIdTable<AuthWebAuthnAuthenticatorRiskPolicy>("auth_webauthn_authenticator_risk_policy") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var blockedRiskLevels = text("blocked_risk_levels").bindTo { it.blockedRiskLevels }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
