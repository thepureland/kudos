package io.kudos.ms.auth.core.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.text
import org.ktorm.schema.varchar

object AuthIdentityProviders : StringIdTable<AuthIdentityProvider>("auth_identity_provider") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var templateId = varchar("template_id").bindTo { it.templateId }
    var code = varchar("code").bindTo { it.code }
    var displayName = varchar("display_name").bindTo { it.displayName }
    var issuer = varchar("issuer").bindTo { it.issuer }
    var clientId = varchar("client_id").bindTo { it.clientId }
    var clientSecretRef = varchar("client_secret_ref").bindTo { it.clientSecretRef }
    var scopes = varchar("scopes").bindTo { it.scopes }
    var customConfig = text("custom_config").bindTo { it.customConfig }
    var jitPolicy = varchar("jit_policy").bindTo { it.jitPolicy }
    var linkPolicy = varchar("link_policy").bindTo { it.linkPolicy }
    var active = boolean("active").bindTo { it.active }
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }
    var createReason = varchar("create_reason").bindTo { it.createReason }
    var createTime = datetime("create_time").bindTo { it.createTime }
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    var updateReason = varchar("update_reason").bindTo { it.updateReason }
    var updateTime = datetime("update_time").bindTo { it.updateTime }
}
