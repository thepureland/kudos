package io.kudos.ms.user.common.account.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * User third-party account form update request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserAccountThirdFormUpdate (

    /** Primary key */
    override val id: String,

    override val userId: String?,

    override val accountProviderDictCode: String?,

    override val accountProviderIssuer: String?,

    override val subject: String?,

    override val unionId: String?,

    override val externalDisplayName: String?,

    override val externalEmail: String?,

    override val avatarUrl: String?,

    override val lastLoginTime: LocalDateTime?,

    override val tenantId: String?,

    override val remark: String?,

) : IIdEntity<String>, IUserAccountThirdFormBase
