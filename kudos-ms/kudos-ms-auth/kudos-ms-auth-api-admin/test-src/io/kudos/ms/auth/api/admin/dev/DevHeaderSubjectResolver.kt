package io.kudos.ms.auth.api.admin.dev

import io.kudos.ms.auth.common.authz.vo.SubjectRef
import io.kudos.ms.auth.core.principal.spi.ISubjectResolver
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

/**
 * Names the caller from an `X-Dev-User` header. **Dev tool only** — it lives in test scope and is
 * registered by [ShadowModeDemoLauncher], never on a deployable classpath.
 *
 * **Why this is needed to verify anything real.** Every enforcement outcome demonstrated so far is a
 * *refusal*: anonymous 401, unregistered 403. The positive path — a subject who holds a role, whose
 * `decide()` returns PERMIT, whose request actually reaches the controller — has only ever run
 * inside unit tests, with the filter and the resolver chain replaced by fakes. That is exactly the
 * seam where an authorization system fails in the embarrassing direction, and it was the one thing
 * the live run could not show.
 *
 * It is also the first real exercise of [ISubjectResolver]: the SPI was written so that a machine
 * or gateway identity could be recognised without borrowing a human session, and until now nothing
 * had ever contributed one.
 *
 * **It authenticates nothing, on purpose.** A header is not proof of identity, and a real resolver
 * must verify (a signature, an mTLS peer, a session). This one exists to answer "does the wiring
 * carry a subject end to end", so it trusts the header — which is precisely why it must never leave
 * test scope. It is ordered ahead of the login-state resolver so the header wins when present.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class DevHeaderSubjectResolver : ISubjectResolver {

    override fun resolve(): SubjectRef? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes ?: return null
        val userId = attributes.request.getHeader(HEADER)?.trim()?.takeIf { it.isNotEmpty() } ?: return null
        val type = attributes.request.getHeader(TYPE_HEADER)?.trim()?.takeIf { it.isNotEmpty() } ?: "USER"
        return SubjectRef(userId, type)
    }

    /** Ahead of the login-state resolver, so an explicit dev identity wins over an ambient session. */
    override fun order(): Int = ISubjectResolver.LOGIN_STATE_ORDER - 100

    companion object {
        const val HEADER = "X-Dev-User"
        const val TYPE_HEADER = "X-Dev-Principal-Type"
    }
}
