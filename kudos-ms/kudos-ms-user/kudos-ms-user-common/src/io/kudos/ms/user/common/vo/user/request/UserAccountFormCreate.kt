package io.kudos.ms.user.common.vo.user.request

import java.time.LocalDateTime


/**
 * 用户表单新建请求VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountFormCreate (

    override val username: String? = null,

    override val tenantId: String? = null,

    override val loginPassword: String? = null,

    override val securityPassword: String? = null,

    override val accountTypeDictCode: String? = null,

    override val accountStatusDictCode: String? = null,

    override val defaultLocale: String? = null,

    override val defaultTimezone: String? = null,

    override val defaultCurrency: String? = null,

    override val lastLoginTime: LocalDateTime? = null,

    override val lastLoginIp: Long? = null,

    override val lastLogoutTime: LocalDateTime? = null,

    override val loginErrorTimes: Int? = null,

    override val securityPasswordErrorTimes: Int? = null,

    override val sessionKey: String? = null,

    override val authenticationKey: String? = null,

    override val orgId: String? = null,

    override val supervisorId: String? = null,

    override val remark: String? = null,

) : IUserAccountFormBase
