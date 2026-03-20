package io.kudos.ms.auth.common.vo.group.request

import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 用户组表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupFormUpdate (

    /** 主键 */
    override val id: String? = null,

    /** 用户组编码 */
    val code: String? = null,

    /** 用户组名称 */
    val name: String? = null,

    /** 租户id */
    val tenantId: String? = null,

    /** 子系统编码 */
    val subsysCode: String? = null,

    /** 备注 */
    val remark: String? = null,

) : IIdEntity<String?>
