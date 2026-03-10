package io.kudos.ms.auth.common.vo.group

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 用户组查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class AuthGroupQuery (

    //region your codes 1

    /** 用户组编码 */
    val code: String? = null,

    /** 用户组名称 */
    val name: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 子系统编码 */
    val subsysCode: String? = null,

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

    override var returnEntityClass: KClass<*>? = AuthGroupRow::class

    //endregion your codes 3

}
