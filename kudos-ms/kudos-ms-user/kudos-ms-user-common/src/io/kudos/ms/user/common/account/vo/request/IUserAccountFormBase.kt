package io.kudos.ms.user.common.account.vo.request
import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import java.time.LocalDateTime

/**
 * 用户表单基础字段（新建 / 更新共用）
 *
 * @author K
 * @since 1.0.0
 */
interface IUserAccountFormBase {

    /** 用户名 */
    val username: String?

    /** 租户ID */
    val tenantId: String?

    /** 登录密码 */
    val loginPassword: String?

    /** 安全密码 */
    val securityPassword: String?

    /** 用户类型字典码 */
    val accountTypeDictCode: String?

    /** 用户状态字典码 */
    val accountStatusDictCode: String?

    /** 默认语言环境 */
    val defaultLocale: String?

    /** 默认时区 */
    val defaultTimezone: String?

    /** 默认货币 */
    val defaultCurrency: String?

    /** 最后登录时间 */
    val lastLoginTime: LocalDateTime?

    /** 最后登录IP */
    val lastLoginIp: Long?

    /** 最后登出时间 */
    val lastLogoutTime: LocalDateTime?

    /** 登录错误次数 */
    val loginErrorTimes: Int?

    /** 安全密码错误次数 */
    val securityPasswordErrorTimes: Int?

    /** 会话密钥 */
    val sessionKey: String?

    /** 认证密钥 */
    val authenticationKey: String?

    /** 所属机构ID */
    val orgId: String?

    /** 直属上级ID */
    val supervisorId: String?

    /** 备注 */
    @get:MaxLength(128)
    val remark: String?
}
