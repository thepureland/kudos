package io.kudos.ms.user.common.vo.user

import io.kudos.base.support.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 用户缓存项
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class UserAccountCacheEntry (

    /** 主键 */
    override val id: String = "",


    /** 用户名 */
    val username: String? = null,

    /** 租户ID */
    val tenantId: String? = null,

    /** 登录密码 */
    val loginPassword: String? = null,

    /** 安全密码 */
    val securityPassword: String? = null,

    /** 用户类型字典码 */
    val accountTypeDictCode: String? = null,

    /** 用户状态字典码 */
    val accountStatusDictCode: String? = null,

    /** 默认语言环境 */
    val defaultLocale: String? = null,

    /** 默认时区 */
    val defaultTimezone: String? = null,

    /** 默认货币 */
    val defaultCurrency: String? = null,

    /** 最后登录时间 */
    val lastLoginTime: LocalDateTime? = null,

    /** 最后登录IP */
    val lastLoginIp: Long? = null,

    /** 最后登出时间 */
    val lastLogoutTime: LocalDateTime? = null,

    /** 登录错误次数 */
    val loginErrorTimes: Int? = null,

    /** 安全密码错误次数 */
    val securityPasswordErrorTimes: Int? = null,

    /** 会话密钥 */
    val sessionKey: String? = null,

    /** 认证密钥 */
    val authenticationKey: String? = null,

    /** 所属机构ID */
    val orgId: String? = null,

    /** 直属上级ID */
    val supervisorId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建者id */
    val createUserId: String? = null,

    /** 创建者名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新者id */
    val updateUserId: String? = null,

    /** 更新者名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

) : IIdEntity<String>, Serializable {


    constructor() : this("")


    companion object {
        private const val serialVersionUID = 1L
    }

}
