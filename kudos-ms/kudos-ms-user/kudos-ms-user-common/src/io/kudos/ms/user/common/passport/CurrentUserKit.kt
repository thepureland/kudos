package io.kudos.ms.user.common.passport

import io.kudos.context.core.KudosContextHolder
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal


/**
 * Utility for reading the "current logged-in user".
 *
 * The data source is [io.kudos.context.core.KudosContext.user], populated from HttpSession by
 * [io.kudos.ms.user.api.public.filter.UserContextWebFilter].
 * Therefore non-null values are only readable in the following scenarios:
 *   1) The request goes through the web filter chain
 *   2) The user has logged in via `POST /api/public/user/passport/login` (which writes the session)
 *
 * In RPC / background tasks / tests the context is empty, so [currentUserIdOrNull] returns null.
 *
 * @author K
 * @since 1.0.0
 */
object CurrentUserKit {

    /** Logged-in user bound to the current thread; returns null if not logged in / no context. */
    fun currentPrincipalOrNull(): SessionUserPrincipal? =
        KudosContextHolder.getOrNull()?.user as? SessionUserPrincipal

    /** Logged-in user id bound to the current thread; returns null if not logged in / no context. */
    fun currentUserIdOrNull(): String? = currentPrincipalOrNull()?.id

    /** Logged-in user id bound to the current thread; throws [IllegalStateException] if not logged in. For code paths that explicitly require login. */
    fun currentUserId(): String =
        currentUserIdOrNull() ?: error("No logged-in user bound to the current thread: not logged in or not passed through UserContextWebFilter")

    /** Tenant id bound to the current thread; returns null if not logged in. */
    fun currentTenantIdOrNull(): String? = currentPrincipalOrNull()?.tenantId

}
