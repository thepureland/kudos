package io.kudos.ms.user.common.vo.user.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.user.common.vo.user.response.UserAccountThirdRow


/**
 * 用户第三方账号列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdQuery (

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

    /** 租户ID */
    val tenantId: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {

    override fun getReturnEntityClass() = UserAccountThirdRow::class

}