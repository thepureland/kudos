package io.kudos.ms.user.common.user.vo.request
import java.time.LocalDateTime


/**
 * 用户第三方账号表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdFormCreate (

    override val userId: String? ,

    override val accountProviderDictCode: String? ,

    override val accountProviderIssuer: String? ,

    override val subject: String? ,

    override val unionId: String? ,

    override val externalDisplayName: String? ,

    override val externalEmail: String? ,

    override val avatarUrl: String? ,

    override val lastLoginTime: LocalDateTime? ,

    override val tenantId: String? ,

    override val remark: String? ,

) : IUserAccountThirdFormBase
