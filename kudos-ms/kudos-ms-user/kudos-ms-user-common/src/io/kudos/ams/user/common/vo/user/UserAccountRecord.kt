package io.kudos.ms.user.common.vo.user

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 用户查询记录
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class UserAccountRecord (

    //region your codes 1

    /** 用户名 */
    var username: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 用户类型字典码 */
    var accountTypeDictCode: String? = null,

    /** 用户状态字典码 */
    var accountStatusDictCode: String? = null,

    /** 默认语言 */
    var defaultLocale: String? = null,

    /** 默认时区 */
    var defaultTimezone: String? = null,

    /** 默认货币 */
    var defaultCurrency: String? = null,

    /** 最后登录时间 */
    var lastLoginTime: LocalDateTime? = null,

    /** 最后登录IP */
    var lastLoginIp: Long? = null,

    /** 最后登出时间 */
    var lastLogoutTime: LocalDateTime? = null,

    /** 登录错误次数 */
    var loginErrorTimes: Int? = null,

    /** 安全密码错误次数 */
    var securityPasswordErrorTimes: Int? = null,

    /** 会话密钥 */
    var sessionKey: String? = null,

    /** 认证密钥 */
    var authenticationKey: String? = null,

    /** 机构id */
    var orgId: String? = null,

    /** 主管id */
    var supervisorId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建者id */
    var createUserId: String? = null,

    /** 创建者名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新者id */
    var updateUserId: String? = null,

    /** 更新者名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(null)

    //endregion your codes 3

}
