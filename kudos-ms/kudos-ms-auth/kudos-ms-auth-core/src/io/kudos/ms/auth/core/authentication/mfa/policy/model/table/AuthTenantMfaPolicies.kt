package io.kudos.ms.auth.core.authentication.mfa.policy.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.mfa.policy.model.po.AuthTenantMfaPolicy
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object AuthTenantMfaPolicies : StringIdTable<AuthTenantMfaPolicy>("auth_tenant_mfa_policy") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var mode = varchar("mode").bindTo { it.mode }
    var gracePeriodDays = int("grace_period_days").bindTo { it.gracePeriodDays }
    var allowedMethods = varchar("allowed_methods").bindTo { it.allowedMethods }
    var recoveryCodesEnabled = boolean("recovery_codes_enabled").bindTo { it.recoveryCodesEnabled }
    var requiredAccountTypeCodes = text("required_account_type_codes").bindTo { it.requiredAccountTypeCodes }
    var requiredRoleCodes = text("required_role_codes").bindTo { it.requiredRoleCodes }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
