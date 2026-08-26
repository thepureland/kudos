package io.kudos.ms.auth.core.authentication

import io.kudos.ms.auth.core.authentication.spi.IAuthenticationMethodProvider
import org.springframework.stereotype.Component

/** Resolves authentication methods without coupling the orchestrator to provider SDKs. */
@Component
open class AuthenticationMethodRegistry(
    providers: List<IAuthenticationMethodProvider>,
) {
    private val byMethod = providers.associateBy { it.method().trim().lowercase() }

    open fun find(method: String): IAuthenticationMethodProvider? = byMethod[method.trim().lowercase()]

    open fun registeredMethods(): Set<String> = byMethod.keys
}
