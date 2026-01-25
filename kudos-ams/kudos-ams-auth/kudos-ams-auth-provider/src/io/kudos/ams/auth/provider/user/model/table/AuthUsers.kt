package io.kudos.ams.auth.provider.user.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ams.auth.provider.user.model.po.AuthUser
import org.ktorm.schema.*


/**
 * 用户基本信息数据库表-实体关联对象
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
object AuthUsers : StringIdTable<AuthUser>("auth_user") {
//endregion your codes 1

    /** 用户名 */
    var username = varchar("username").bindTo { it.username }

    /** 租户ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** 登录密码 */
    var loginPassword = varchar("login_password").bindTo { it.loginPassword }

    /** 安全密码 */
    var securityPassword = varchar("security_password").bindTo { it.securityPassword }

    /** 用户类型字典码 */
    var userTypeDictCode = varchar("user_type_dict_code").bindTo { it.userTypeDictCode }

    /** 用户状态字典码 */
    var userStatusDictCode = varchar("user_status_dict_code").bindTo { it.userStatusDictCode }

    /** 默认语言环境 */
    var defaultLocale = varchar("default_locale").bindTo { it.defaultLocale }

    /** 默认时区 */
    var defaultTimezone = varchar("default_timezone").bindTo { it.defaultTimezone }

    /** 默认货币 */
    var defaultCurrency = varchar("default_currency").bindTo { it.defaultCurrency }

    /** 最后登录时间 */
    var lastLoginTime = datetime("last_login_time").bindTo { it.lastLoginTime }

    /** 最后登录IP */
    var lastLoginIp = long("last_login_ip").bindTo { it.lastLoginIp }

    /** 最后登出时间 */
    var lastLogoutTime = datetime("last_logout_time").bindTo { it.lastLogoutTime }

    /** 登录错误次数 */
    var loginErrorTimes = int("login_error_times").bindTo { it.loginErrorTimes }

    /** 安全密码错误次数 */
    var securityPasswordErrorTimes = int("security_password_error_times").bindTo { it.securityPasswordErrorTimes }

    /** 会话密钥 */
    var sessionKey = varchar("session_key").bindTo { it.sessionKey }

    /** 认证密钥 */
    var authenticationKey = varchar("authentication_key").bindTo { it.authenticationKey }

    /** 所属部门ID */
    var deptId = varchar("dept_id").bindTo { it.deptId }

    /** 直属上级ID */
    var supervisorId = varchar("supervisor_id").bindTo { it.supervisorId }

    /** 备注 */
    var remark = varchar("remark").bindTo { it.remark }

    /** 是否激活 */
    var active = boolean("active").bindTo { it.active }

    /** 是否内置 */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** 创建者id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** 创建者名称 */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** 创建时间 */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** 更新者id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** 更新者名称 */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** 更新时间 */
    var updateTime = datetime("update_time").bindTo { it.updateTime }


    //region your codes 2

    //endregion your codes 2

}
