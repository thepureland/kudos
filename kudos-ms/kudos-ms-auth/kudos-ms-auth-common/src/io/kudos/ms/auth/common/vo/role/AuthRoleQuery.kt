package io.kudos.ms.auth.common.vo.role

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 角色查询条件载体
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
data class AuthRoleQuery (


    /** 角色编码 */
    val code: String? = null,

    /** 角色名称 */
    val name: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 子系统编码 */
    val subsysCode: String? = null,

    /** 备注 */
    val remark: String? = null,

    /** 是否激活 */
    val active: Boolean? = null,

    /** 是否内置 */
    val builtIn: Boolean? = null,

) : ListSearchPayload() {


    constructor() : this("")

    override var returnEntityClass: KClass<*>? = AuthRoleRow::class


}
