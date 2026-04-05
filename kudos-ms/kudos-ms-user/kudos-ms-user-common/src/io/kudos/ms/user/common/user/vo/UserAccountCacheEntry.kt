package io.kudos.ms.user.common.user.vo
import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 用户缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountCacheEntry (

    /** 主键 */
    override val id: String,

    /** 用户名 */
    val username: String?,

    /** 租户ID */
    val tenantId: String?,

    /** 登录密码 */
    val loginPassword: String?,

    /** 安全密码 */
    val securityPassword: String?,

    /** 用户类型字典码 */
    val accountTypeDictCode: String?,

    /** 用户状态字典码 */
    val accountStatusDictCode: String?,

    /** 默认语言环境 */
    val defaultLocale: String?,

    /** 默认时区 */
    val defaultTimezone: String?,

    /** 默认货币 */
    val defaultCurrency: String?,

    /** 最后登录时间 */
    val lastLoginTime: LocalDateTime?,

    /** 最后登录IP */
    val lastLoginIp: Long?,

    /** 最后登出时间 */
    val lastLogoutTime: LocalDateTime?,

    /** 登录错误次数 */
    val loginErrorTimes: Int?,

    /** 安全密码错误次数 */
    val securityPasswordErrorTimes: Int?,

    /** 会话密钥 */
    val sessionKey: String?,

    /** 认证密钥 */
    val authenticationKey: String?,

    /** 所属机构ID */
    val orgId: String?,

    /** 直属上级ID */
    val supervisorId: String?,

    /** 备注 */
    val remark: String?,

    /** 是否激活 */
    val active: Boolean?,

    /** 是否内置 */
    val builtIn: Boolean?,

    /** 创建者id */
    val createUserId: String?,

    /** 创建者名称 */
    val createUserName: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新者id */
    val updateUserId: String?,

    /** 更新者名称 */
    val updateUserName: String?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
