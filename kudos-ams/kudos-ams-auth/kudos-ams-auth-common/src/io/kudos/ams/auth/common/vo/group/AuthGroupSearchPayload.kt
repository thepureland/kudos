package io.kudos.ams.auth.common.vo.group

import io.kudos.base.support.payload.ListSearchPayload
import kotlin.reflect.KClass


/**
 * 用户组查询条件载体
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
data class AuthGroupSearchPayload (

    //region your codes 1

    override var returnEntityClass: KClass<*>? = AuthGroupRecord::class,

    /** 用户组编码 */
    var code: String? = null,

    /** 用户组名称 */
    var name: String? = null,

    /** 租户id */
    var tenantId: String? = null,

    /** 子系统编码 */
    var subsysCode: String? = null,

    /** 是否激活 */
    var active: Boolean? = null,

    /** 是否内置 */
    var builtIn: Boolean? = null,

    //endregion your codes 1
//region your codes 2
) : ListSearchPayload() {
//endregion your codes 2

    //region your codes 3

    constructor() : this(AuthGroupRecord::class)

    //endregion your codes 3

}
