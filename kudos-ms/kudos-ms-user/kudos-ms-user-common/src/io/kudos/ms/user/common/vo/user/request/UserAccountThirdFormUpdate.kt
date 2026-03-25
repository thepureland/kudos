package io.kudos.ms.user.common.vo.user.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * 用户第三方账号表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 关联用户账号ID */
    val userId: String? = null,

    /** 第三方平台字典码 */
    val accountProviderDictCode: String? = null,

    /** 发行方/平台租户 */
    val accountProviderIssuer: String? = null,

    /** 第三方用户唯一标识 */
    val subject: String? = null,

    /** 跨应用统一标识 */
    val unionId: String? = null,

    /** 第三方展示名 */
    val externalDisplayName: String? = null,

    /** 第三方邮箱 */
    val externalEmail: String? = null,

    /** 头像URL */
    val avatarUrl: String? = null,

    /** 最后登录时间 */
    val lastLoginTime: LocalDateTime? = null,

    /** 租户ID */
    val tenantId: String? = null,

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

) : IIdEntity<String?>
