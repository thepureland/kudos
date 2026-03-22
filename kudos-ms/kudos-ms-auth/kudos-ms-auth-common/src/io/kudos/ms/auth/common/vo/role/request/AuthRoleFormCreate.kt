package io.kudos.ms.auth.common.vo.role.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/**
 * 角色表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleFormCreate (

    /** 角色编码 */
    val code: String? = null,

    /** 角色名称 */
    val name: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 子系统编码 */
    val subsysCode: String? = null,

    /** 备注 */
    @get:MaxLength(128)
    val remark: String? = null,

)
