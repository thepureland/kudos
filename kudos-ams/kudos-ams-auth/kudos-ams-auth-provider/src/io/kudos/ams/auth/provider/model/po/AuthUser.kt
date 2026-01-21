package io.kudos.ams.auth.provider.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * 用户基本信息数据库实体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
//region your codes 1
interface AuthUser : IDbEntity<String, AuthUser> {
//endregion your codes 1

    companion object : DbEntityFactory<AuthUser>()

    /** 用户名 */
    var username: String

    /** 租户ID */
    var tenantId: String

    /** 登录密码 */
    var loginPassword: String

    /** 安全密码 */
    var securityPassword: String?

    /** 用户类型字典码 */
    var userTypeDictCode: String?

    /** 用户状态字典码 */
    var userStatusDictCode: String?

    /** 默认语言环境 */
    var defaultLocale: String?

    /** 默认时区 */
    var defaultTimezone: String?

    /** 默认货币 */
    var defaultCurrency: String?

    /** 最后登录时间 */
    var lastLoginTime: LocalDateTime?

    /** 最后登录IP */
    var lastLoginIp: Long?

    /** 最后登出时间 */
    var lastLogoutTime: LocalDateTime?

    /** 登录错误次数 */
    var loginErrorTimes: Int?

    /** 安全密码错误次数 */
    var securityPasswordErrorTimes: Int?

    /** 会话密钥 */
    var sessionKey: String?

    /** 认证密钥 */
    var authenticationKey: String?

    /** 所属部门ID */
    var deptId: String?

    /** 直属上级ID */
    var supervisorId: String

    /** 备注 */
    var remark: String?

    /** 是否激活 */
    var active: Boolean

    /** 是否内置 */
    var builtIn: Boolean?

    /** 创建者id */
    var createUserId: String?

    /** 创建者名称 */
    var createUserName: String?

    /** 创建时间 */
    var createTime: LocalDateTime?

    /** 更新者id */
    var updateUserId: String?

    /** 更新者名称 */
    var updateUserName: String?

    /** 更新时间 */
    var updateTime: LocalDateTime?


    //region your codes 2

    //endregion your codes 2

}
