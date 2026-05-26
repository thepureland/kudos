package io.kudos.ms.user.common.login.vo.response

import io.kudos.base.model.contract.entity.IIdEntity
import java.time.LocalDateTime


/**
 * Remember-me login edit response VO
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeEdit (

    /** Primary key */
    override val id: String = "",

    /** Username */
    val username: String? = null,

    /** Token */
    val token: String? = null,

    /** Last used time */
    val lastUsed: LocalDateTime? = null,

) : IIdEntity<String>
