package io.kudos.ms.auth.common.group.vo.request

/**
 * Request VO for user group form creation.
 *
 * @author K
 * @since 1.0.0
 */
data class AuthGroupFormCreate (

    override val code: String? ,

    override val name: String? ,

    override val tenantId: String? ,

    override val subsysCode: String? ,

    override val remark: String? ,

) : IAuthGroupFormBase
