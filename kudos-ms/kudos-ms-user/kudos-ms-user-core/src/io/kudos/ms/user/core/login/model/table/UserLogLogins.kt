package io.kudos.ms.user.core.login.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.login.model.po.UserLogLogin
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * Login log table-entity binding.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object UserLogLogins : StringIdTable<UserLogLogin>("user_log_login") {

    /** User id */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Username */
    var username = varchar("username").bindTo { it.username }

    /** Tenant id */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Login time */
    var loginTime = datetime("login_time").bindTo { it.loginTime }

    /** Login IP */
    var loginIp = long("login_ip").bindTo { it.loginIp }

    /** Login location */
    var loginLocation = varchar("login_location").bindTo { it.loginLocation }

    /** Login device */
    var loginDevice = varchar("login_device").bindTo { it.loginDevice }

    /** Browser */
    var loginBrowser = varchar("login_browser").bindTo { it.loginBrowser }

    /** Operating system */
    var loginOs = varchar("login_os").bindTo { it.loginOs }

    /** User agent string */
    var userAgent = varchar("user_agent").bindTo { it.userAgent }

    /** Whether the login succeeded */
    var loginSuccess = boolean("login_success").bindTo { it.loginSuccess }

    /** Failure reason */
    var failureReason = varchar("failure_reason").bindTo { it.failureReason }

    /** Session id */
    var sessionId = varchar("session_id").bindTo { it.sessionId }

    /** Remark */
    var remark = varchar("remark").bindTo { it.remark }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }




}
