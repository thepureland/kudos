package io.kudos.ms.user.common.vo.protection

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 用户账号保护查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class UserAccountProtectionSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = UserAccountProtectionRecord::class,

    /** 用户ID */
    var userId: String? = null,

    /** 是否启用 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(UserAccountProtectionRecord::class)

    //endregion your codes 3

}
