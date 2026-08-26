package io.kudos.ms.auth.core.authentication.loginevent.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.auth.core.authentication.loginevent.model.po.AuthLoginEvent
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

object AuthLoginEvents : StringIdTable<AuthLoginEvent>("auth_login_event") {
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }
    var userId = varchar("user_id").bindTo { it.userId }
    var identifierHash = varchar("identifier_hash").bindTo { it.identifierHash }
    var providerId = varchar("provider_id").bindTo { it.providerId }
    var authenticationMethod = varchar("authentication_method").bindTo { it.authenticationMethod }
    var transactionId = varchar("transaction_id").bindTo { it.transactionId }
    var purpose = varchar("purpose").bindTo { it.purpose }
    var sessionId = varchar("session_id").bindTo { it.sessionId }
    var success = boolean("success").bindTo { it.success }
    var failureCode = varchar("failure_code").bindTo { it.failureCode }
    var acr = varchar("acr").bindTo { it.acr }
    var amr = varchar("amr").bindTo { it.amr }
    var loginIp = long("login_ip").bindTo { it.loginIp }
    var loginLocation = varchar("login_location").bindTo { it.loginLocation }
    var loginDevice = varchar("login_device").bindTo { it.loginDevice }
    var loginBrowser = varchar("login_browser").bindTo { it.loginBrowser }
    var loginOs = varchar("login_os").bindTo { it.loginOs }
    var userAgent = varchar("user_agent").bindTo { it.userAgent }
    var occurredAt = datetime("occurred_at").bindTo { it.occurredAt }
}
