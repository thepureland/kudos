package io.kudos.ms.user.common.login.vo.request

import java.time.LocalDateTime

/**
 * Remember-me login form base fields (shared between create and update)
 *
 * @author K
 * @since 1.0.0
 */
interface IUserLoginRememberMeFormBase {

    /** Username */
    val username: String?

    /** Token */
    val token: String?

    /** Last used time */
    val lastUsed: LocalDateTime?
}
