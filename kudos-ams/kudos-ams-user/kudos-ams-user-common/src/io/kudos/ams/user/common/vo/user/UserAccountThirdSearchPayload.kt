package io.kudos.ams.user.common.vo.user

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 用户第三方账号查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserAccountThirdSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = UserAccountThirdRecord::class,

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

    /** 租户ID */
    var tenantId: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(UserAccountThirdRecord::class)

    //endregion your codes 3

}
