package io.kudos.ms.auth.core.platform.authz.enforcement

import io.kudos.ability.security.enforcement.port.IAuthzDecisionProvider
import io.kudos.ms.auth.common.authz.api.IAuthzDecisionApi
import io.kudos.ms.auth.common.authz.vo.AuthzRequest
import io.kudos.ms.auth.core.principal.CurrentSubjectResolver
import jakarta.annotation.Resource
import org.springframework.stereotype.Component


/**
 * Connects the enforcement layer's port to kudos' own decision point.
 *
 * The subject is read from ambient request state here, and nowhere else: enforcement points pass
 * only the required permission code, so there is no path by which a caller could have a check
 * performed as somebody other than themselves.
 *
 * Who that subject is goes through [CurrentSubjectResolver] rather than straight to the login state,
 * so a machine caller — a service token, an API key, an mTLS identity — is enforced as *itself*
 * rather than having to borrow a human account.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Component
open class AuthzDecisionProviderAdapter : IAuthzDecisionProvider {

    @Resource
    private lateinit var authzDecisionApi: IAuthzDecisionApi

    @Resource
    private lateinit var currentSubjectResolver: CurrentSubjectResolver

    override fun isPermitted(permissionCode: String, context: Map<String, Any?>): Boolean {
        val subject = currentSubjectResolver.currentSubject() ?: return false
        return authzDecisionApi.decide(AuthzRequest(subject, permissionCode, context)).permitted
    }

    override fun hasSubject(): Boolean = currentSubjectResolver.currentSubject() != null
}
