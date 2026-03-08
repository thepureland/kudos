package io.kudos.ms.user.common.vo.user

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 用户查询条件载体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class UserAccountSearchPayload (

    //region your codes 1

    /** 用户名 */
    val username: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 用户类型字典码 */
    val accountTypeDictCode: String? = null,

    /** 用户状态字典码 */
    val accountStatusDictCode: String? = null,

    /** 机构id */
    val orgId: String? = null,

    /** 主管id */
    val supervisorId: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this("")

    override var returnEntityClass: KClass<*>? = UserAccountRecord::class

    //endregion your codes 3

}
