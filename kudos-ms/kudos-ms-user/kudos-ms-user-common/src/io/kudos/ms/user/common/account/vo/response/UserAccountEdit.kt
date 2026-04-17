package io.kudos.ms.user.common.account.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 用户编辑响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountEdit (

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

) : IIdEntity<String>
