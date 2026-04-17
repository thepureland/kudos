package io.kudos.ms.user.common.account.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * 用户第三方账号缓存项
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdCacheEntry (

    /** 主键 */
    override val id: String,

    /** 关联用户账号ID */
    val userId: String?,

    /** 第三方平台字典码 */
    val accountProviderDictCode: String?,

    /** 发行方/平台租户 */
    val accountProviderIssuer: String?,

    /** 第三方用户唯一标识 */
    val subject: String?,

    /** 跨应用统一标识 */
    val unionId: String?,

    /** 第三方展示名 */
    val externalDisplayName: String?,

    /** 第三方邮箱 */
    val externalEmail: String?,

    /** 头像URL */
    val avatarUrl: String?,

    /** 最后登录时间 */
    val lastLoginTime: LocalDateTime?,

    /** 租户ID */
    val tenantId: String?,

    /** 备注 */
    val remark: String?,

    /** 是否激活 */
    val active: Boolean?,

    /** 是否内置 */
    val builtIn: Boolean?,

    /** 创建用户ID */
    val createUserId: String?,

    /** 创建用户名称 */
    val createUserName: String?,

    /** 创建时间 */
    val createTime: LocalDateTime?,

    /** 更新用户ID */
    val updateUserId: String?,

    /** 更新用户名称 */
    val updateUserName: String?,

    /** 更新时间 */
    val updateTime: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
