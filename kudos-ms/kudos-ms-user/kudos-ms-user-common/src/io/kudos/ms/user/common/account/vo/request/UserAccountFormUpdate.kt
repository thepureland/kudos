package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * User account form update request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountFormUpdate (

    /** Primary key */
    override val id: String,

    override val username: String?,

    override val tenantId: String?,

    override val loginPassword: String?,

    override val securityPassword: String?,

    override val accountTypeDictCode: String?,

    override val accountStatusDictCode: String?,

    override val defaultLocale: String?,

    override val defaultTimezone: String?,

    override val defaultCurrency: String?,

    override val lastLoginTime: LocalDateTime?,

    override val lastLoginIp: Long?,

    override val lastLogoutTime: LocalDateTime?,

    override val loginErrorTimes: Int?,

    override val securityPasswordErrorTimes: Int?,

    override val sessionKey: String?,

    override val authenticationKey: String?,

    override val orgId: String?,

    override val supervisorId: String?,

    override val remark: String?,

) : IIdEntity<String>, IUserAccountFormBase
