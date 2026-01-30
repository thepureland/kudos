package io.kudos.ams.user.provider.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.user.provider.model.po.UserLogLogin
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.long
import org.ktorm.schema.varchar

/**
 * 登录日志数据库表-实体关联对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
object UserLogLogins : StringIdTable<UserLogLogin>("user_log_login") {
//endregion your codes 1

    /** 用户ID */
    var userId = varchar("user_id").bindTo { it.userId }

    /** 用户名 */
    var username = varchar("username").bindTo { it.username }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 登录时间 */
    var loginTime = datetime("login_time").bindTo { it.loginTime }

    /** 登录IP */
    var loginIp = long("login_ip").bindTo { it.loginIp }

    /** 登录地点 */
    var loginLocation = varchar("login_location").bindTo { it.loginLocation }

    /** 登录设备 */
    var loginDevice = varchar("login_device").bindTo { it.loginDevice }

    /** 浏览器 */
    var loginBrowser = varchar("login_browser").bindTo { it.loginBrowser }

    /** 操作系统 */
    var loginOs = varchar("login_os").bindTo { it.loginOs }

    /** 用户代理字符串 */
    var userAgent = varchar("user_agent").bindTo { it.userAgent }

    /** 是否登录成功 */
    var loginSuccess = boolean("login_success").bindTo { it.loginSuccess }

    /** 失败原因 */
    var failureReason = varchar("failure_reason").bindTo { it.failureReason }

    /** 会话ID */
    var sessionId = varchar("session_id").bindTo { it.sessionId }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }


    //region your codes 2

    //endregion your codes 2

}