package io.kudos.ms.auth.core.principal

import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.principal.spi.ISubjectResolver
import io.kudos.ms.user.common.passport.CurrentUserKit
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * The built-in resolver: the logged-in human, from request-bound login state.
 *
 * Ordered last on purpose. A machine caller usually also arrives on a session-capable transport, and
 * if the human resolver ran first it would answer for the request whenever any session existed —
 * silently attributing a service's actions to whoever last logged in on that connection.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class LoginStateSubjectResolver : ISubjectResolver {

    override fun resolve(): SubjectRef? =
        CurrentUserKit.currentUserIdOrNull()?.let { SubjectRef.ofUser(it) }

    override fun order(): Int = ISubjectResolver.LOGIN_STATE_ORDER
}


/**
 * Asks every registered [ISubjectResolver] in order and returns the first answer.
 *
 * One place decides who the caller is, so every enforcement point — the URL filter, the method
 * aspect, row filtering — agrees, and adding a principal kind changes none of them.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class CurrentSubjectResolver {

    @Resource
    private lateinit var resolvers: List<ISubjectResolver>

    /**
     * The subject of the current request.
     *
     * @return the subject, or null when there is no authenticated caller at all
     */
    open fun currentSubject(): SubjectRef? =
        resolvers.sortedBy { it.order() }.firstNotNullOfOrNull { it.resolve() }
}
