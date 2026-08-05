package io.kudos.ms.auth.core.platform.authz.enforcement

import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator
import io.kudos.ability.security.enforcement.port.ITokenFreshnessValidator.Freshness
import io.kudos.ms.auth.common.authz.api.IPermissionVersionApi
import io.kudos.ms.auth.core.principal.CurrentSubjectResolver
import jakarta.annotation.Resource
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes


/**
 * Connects the enforcement layer's freshness port to the permission version.
 *
 * **Where the presented version comes from.** A request header, named by configuration and defaulting
 * to `X-Permission-Version`. That may look indirect when the version is conceptually a token claim,
 * and it is deliberate: this module must not know how a deployment encodes its credentials — JWT
 * claim, opaque token looked up by a gateway, mTLS-derived identity — and every one of those
 * arrangements can put a header on the request it forwards. A deployment that would rather read a
 * claim directly replaces this bean; the port is the contract, not the header.
 *
 * The version is compared, never trusted: a caller can of course put any value in the header, and the
 * only thing that gets them is [Freshness.FRESH] for a version they would have had anyway. It grants
 * nothing on its own — the decision point still runs afterwards.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class PermissionVersionFreshnessValidator : ITokenFreshnessValidator {

    @Resource
    private lateinit var permissionVersionApi: IPermissionVersionApi

    @Resource
    private lateinit var currentSubjectResolver: CurrentSubjectResolver

    /** Header carrying the version the credential was minted with. */
    @Value("\${kudos.ms.auth.authz.permission-version-header:X-Permission-Version}")
    private var versionHeader: String = "X-Permission-Version"

    override fun assess(): Freshness {
        // No subject is not a freshness question — the filter already answers it with a 401, and
        // reporting STALE here would replace a clear "you are not logged in" with a confusing
        // "your session is out of date".
        val subject = currentSubjectResolver.currentSubject() ?: return Freshness.UNKNOWN
        val presented = currentRequestHeader(versionHeader)?.trim()?.takeIf { it.isNotEmpty() }
            ?: return Freshness.UNKNOWN
        return if (permissionVersionApi.isCurrent(subject, presented)) Freshness.FRESH else Freshness.STALE
    }

    /**
     * Null outside a servlet request — a scheduled job or a message consumer reaches the same beans
     * and has no credential to assess.
     */
    private fun currentRequestHeader(name: String): String? {
        val attributes = RequestContextHolder.getRequestAttributes() as? ServletRequestAttributes
        return attributes?.request?.getHeader(name)
    }
}
