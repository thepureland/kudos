package io.kudos.ms.user.common.passport.vo

import io.kudos.base.model.contract.entity.IIdEntity
import java.io.Serializable


/**
 * Snapshot of the "current user" stored in the login session.
 *
 * Two purposes after persistence:
 *   1) Serialized into `HttpSession[KudosContext.SESSION_KEY_USER]` (servlet session uses Java serialization by default)
 *   2) Read out by [io.kudos.ms.user.api.public.filter.UserContextWebFilter] and populated into
 *      [io.kudos.context.core.KudosContext.user], for existing callers such as
 *      `KudosContextHolder.get().user?.id`
 *
 * Implements [IIdEntity] because the `KudosContext.user` field is of type `IIdEntity<String>?`.
 *
 * @author K
 * @since 1.0.0
 */
data class SessionUserPrincipal(

    override val id: String,

    val tenantId: String,

    val username: String,

) : IIdEntity<String>, Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
