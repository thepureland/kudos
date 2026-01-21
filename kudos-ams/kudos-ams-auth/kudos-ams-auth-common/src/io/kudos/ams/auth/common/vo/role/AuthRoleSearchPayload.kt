package io.kudos.ams.auth.common.vo.role

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 角色查询条件载体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthRoleSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = AuthRoleRecord::class,

    /** 角色编码 */
    var code: String? = null,

    /** 角色名称 */
    var name: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 子系统编码 */
    var subsysCode: String? = null,

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

    constructor() : this(AuthRoleRecord::class)

    //endregion your codes 3

}
