package io.kudos.ams.user.common.vo.user

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

    override var returnEntityClass: KClass<*>? = UserAccountRecord::class,

    /** 用户名 */
    var username: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 用户类型字典码 */
    var accountTypeDictCode: String? = null,

    /** 用户状态字典码 */
    var accountStatusDictCode: String? = null,

    /** 机构id */
    var orgId: String? = null,

    /** 主管id */
    var supervisorId: String? = null,

    /** 备注 */
    var remark: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(UserAccountRecord::class)

    //endregion your codes 3

}
