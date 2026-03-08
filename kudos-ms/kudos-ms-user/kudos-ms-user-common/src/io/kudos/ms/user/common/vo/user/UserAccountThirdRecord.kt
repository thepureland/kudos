package io.kudos.ms.user.common.vo.user

import io.kudos.base.support.result.IdJsonResult
import java.time.LocalDateTime


/**
 * 用户第三方账号查询记录
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserAccountThirdRecord (

    //region your codes 1

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
    val remark: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    /** 创建用户ID */
    val createUserId: String? = null,

    /** 创建用户名称 */
    val createUserName: String? = null,

    /** 创建时间 */
    val createTime: LocalDateTime? = null,

    /** 更新用户ID */
    val updateUserId: String? = null,

    /** 更新用户名称 */
    val updateUserName: String? = null,

    /** 更新时间 */
    val updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}
