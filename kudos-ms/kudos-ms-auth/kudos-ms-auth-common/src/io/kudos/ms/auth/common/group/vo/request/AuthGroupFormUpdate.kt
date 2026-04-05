package io.kudos.ms.auth.common.group.vo.request
import io.kudos.base.model.contract.entity.IIdEntity


/**
 * 用户组表单更新请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupFormUpdate (

    /** 主键 */
    override val id: String,

    override val code: String?,

    override val name: String?,

    override val tenantId: String?,

    override val subsysCode: String?,

    override val remark: String?,

) : IIdEntity<String>, IAuthGroupFormBase
