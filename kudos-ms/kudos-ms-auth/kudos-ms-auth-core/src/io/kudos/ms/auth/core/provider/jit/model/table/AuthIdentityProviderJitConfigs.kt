package io.kudos.ms.auth.core.provider.jit.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.provider.jit.model.po.AuthIdentityProviderJitConfig
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object AuthIdentityProviderJitConfigs :
    StringIdTable<AuthIdentityProviderJitConfig>("auth_identity_provider_jit_config") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var usernameStrategy = varchar("username_strategy").bindTo { it.usernameStrategy }
    var requireVerifiedEmail = boolean("require_verified_email").bindTo { it.requireVerifiedEmail }
    var allowedEmailDomains = text("allowed_email_domains").bindTo { it.allowedEmailDomains }
    var defaultOrgId = varchar("default_org_id").bindTo { it.defaultOrgId }
    var defaultSupervisorId = varchar("default_supervisor_id").bindTo { it.defaultSupervisorId }
    var accountTypeDictCode = varchar("account_type_dict_code").bindTo { it.accountTypeDictCode }
    var accountStatusDictCode = varchar("account_status_dict_code").bindTo { it.accountStatusDictCode }
    var defaultLocale = varchar("default_locale").bindTo { it.defaultLocale }
    var defaultTimezone = varchar("default_timezone").bindTo { it.defaultTimezone }
    var defaultCurrency = varchar("default_currency").bindTo { it.defaultCurrency }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
