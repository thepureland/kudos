package io.kudos.ms.auth.common.role.vo.response

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 角色编辑响应VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthRoleEdit (

    /** 主键 */
    override val id: String = "",

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

) : IIdEntity<String>
