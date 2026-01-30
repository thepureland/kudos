package io.kudos.ams.user.common.vo.user

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
    var userAccountId: String? = null,

    /** 第三方平台字典码 */
    var accountProviderDictCode: String? = null,

    /** 发行方/平台租户 */
    var providerIssuer: String? = null,

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

    /** 子系统代码 */
    var subSysDictCode: String? = null,

    /** 租户ID */
    var tenantId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    /** 创建用户 */
    var createUser: String? = null,

    /** 创建时间 */
    var createTime: LocalDateTime? = null,

    /** 更新用户 */
    var updateUser: String? = null,

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
