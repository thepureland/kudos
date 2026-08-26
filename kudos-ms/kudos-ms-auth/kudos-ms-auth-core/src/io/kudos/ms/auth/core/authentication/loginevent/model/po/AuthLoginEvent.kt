package io.kudos.ms.auth.core.authentication.loginevent.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/** One authentication outcome; append-only, one row per authentication transaction. */
interface AuthLoginEvent : IDbEntity<String, AuthLoginEvent> {
    companion object : DbEntityFactory<AuthLoginEvent>()

    var tenantId: String?
    var userId: String?
    var identifierHash: String?
    var providerId: String?
    var authenticationMethod: String?
    var transactionId: String
    var purpose: String
    var sessionId: String?
    var success: Boolean
    var failureCode: String?
    var acr: String?
    var amr: String?
    var loginIp: Long?
    var loginLocation: String?
    var loginDevice: String?
    var loginBrowser: String?
    var loginOs: String?
    var userAgent: String?
    var occurredAt: LocalDateTime
}
