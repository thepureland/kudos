package io.kudos.ms.auth.common.group.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.auth.common.group.vo.response.AuthGroupRow


/**
 * 用户组列表查询条件请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupQuery (

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

) : ListSearchPayload() {

    override fun getReturnEntityClass() = AuthGroupRow::class

}