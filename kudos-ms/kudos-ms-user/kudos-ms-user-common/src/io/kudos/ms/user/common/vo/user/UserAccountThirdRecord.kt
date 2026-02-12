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
    var userId: String? = null,

    /** 第三方平台字典码 */
    var accountProviderDictCode: String? = null,

    /** 发行方/平台租户 */
    var accountProviderIssuer: String? = null,

    /** 第三方用户唯一标识 */
    var subject: String? = null,

    /** 跨应用统一标识 */
    var unionId: String? = null,

    /** 第三方展示名 */
    var externalDisplayName: String? = null,

    /** 第三方邮箱 */
    var externalEmail: String? = null,

    /** 头像URL */
    var avatarUrl: String? = null,

    /** 最后登录时间 */
    var lastLoginTime: LocalDateTime? = null,

    /** 租户ID */
    var tenantId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建用户ID */
    var createUserId: String? = null,

    /** 创建用户名称 */
    var createUserName: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新用户ID */
    var updateUserId: String? = null,

    /** 更新用户名称 */
    var updateUserName: String? = null,

    /** 更新时间 */
    var updateTime: LocalDateTime? = null,

    //endregion your codes 1
//region your codes 2
) : IdJsonResult<String>() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    //endregion your codes 3

}
