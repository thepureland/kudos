package io.kudos.ams.auth.common.vo.user

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 用户查询条件载体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthUserSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = AuthUserRecord::class,

    /** 用户名 */
    var username: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 用户类型字典码 */
    var userTypeDictCode: String? = null,

    /** 用户状态字典码 */
    var userStatusDictCode: String? = null,

    /** 部门id */
    var deptId: String? = null,

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

    constructor() : this(AuthUserRecord::class)

    //endregion your codes 3

}
