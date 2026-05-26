package io.kudos.ms.user.core.login.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Login log database entity
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface UserLogLogin : IDbEntity<String, UserLogLogin> {

    companion object : DbEntityFactory<UserLogLogin>()

    /** User ID */
    var userId: String

    /** Username */
    var username: String

    /** Tenant ID */
    var tenantId: String

    /** Login time */
    var loginTime: LocalDateTime

    /** Login IP */
    var loginIp: Long?

    /** Login location */
    var loginLocation: String?

    /** Login device */
    var loginDevice: String?

    /** Browser */
    var loginBrowser: String?

    /** Operating system */
    var loginOs: String?

    /** User agent string */
    var userAgent: String?

    /** Whether login succeeded */
    var loginSuccess: Boolean

    /** Failure reason */
    var failureReason: String?

    /** Session ID */
    var sessionId: String?

    /** Remark */
    var remark: String?

    /** Create time */
    var createTime: LocalDateTime?




}