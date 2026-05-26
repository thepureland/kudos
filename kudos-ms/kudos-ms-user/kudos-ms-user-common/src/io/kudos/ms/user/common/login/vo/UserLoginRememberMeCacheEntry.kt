package io.kudos.ms.user.common.login.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable
import java.time.LocalDateTime


/**
 * Remember-me login cache entry
 *
 * @author K
 * @since 1.0.0
 */
data class UserLoginRememberMeCacheEntry (

    /** Primary key */
    override val id: String,

    /** Username */
    val username: String?,

    /** Token */
    val token: String?,

    /** Last used time */
    val lastUsed: LocalDateTime?,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }

}
