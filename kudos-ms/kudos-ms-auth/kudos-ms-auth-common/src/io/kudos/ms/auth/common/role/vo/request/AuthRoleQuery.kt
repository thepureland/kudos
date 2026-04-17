package io.kudos.ms.auth.common.role.vo.request

import io.kudos.base.model.payload.ListSearchPayload
import io.kudos.ms.auth.common.role.vo.response.AuthRoleRow


/**
 * 角色列表查询条件请求VO
 *
 * @author K
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

    override fun getReturnEntityClass() = AuthRoleRow::class

}