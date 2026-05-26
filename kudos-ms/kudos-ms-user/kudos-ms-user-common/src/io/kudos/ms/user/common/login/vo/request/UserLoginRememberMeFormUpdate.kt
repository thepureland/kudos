package io.kudos.ms.user.common.login.vo.request

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Remember-me login form update request VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeFormUpdate (

    /** Primary key */
    override val id: String,

    override val username: String?,

    override val token: String?,

    override val lastUsed: LocalDateTime?,

) : IIdEntity<String>, IUserLoginRememberMeFormBase
