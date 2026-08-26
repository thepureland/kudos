package io.kudos.ms.auth.provider.oauth2.secret

import org.springframework.core.env.Environment
import org.springframework.stereotype.Component
import io.kudos.ms.auth.provider.oauth2.init.ExternalLoginProperties

/** Built-in resolver for environment variables and Spring configuration properties. */
@Component
open class EnvironmentClientSecretResolver(
    private val environment: Environment,
    private val properties: ExternalLoginProperties,
) : IClientSecretResolver {

    override fun supports(reference: String): Boolean =
        reference.startsWith(ENV_PREFIX) || reference.startsWith(PROPERTY_PREFIX)

    override fun resolve(reference: String): String? = when {
        reference.startsWith(ENV_PREFIX) -> {
            val name = reference.removePrefix(ENV_PREFIX)
            requireAllowed(name, ENV_NAME_PATTERN, properties.allowedSecretEnvironmentPrefixes)
            System.getenv(name)
        }
        reference.startsWith(PROPERTY_PREFIX) -> {
            val name = reference.removePrefix(PROPERTY_PREFIX)
            requireAllowed(name, PROPERTY_NAME_PATTERN, properties.allowedSecretPropertyPrefixes)
            environment.getProperty(name)
        }
        else -> null
    }?.takeIf { it.isNotBlank() }

    private fun requireAllowed(name: String, pattern: Regex, allowedPrefixes: Collection<String>) {
        if (!pattern.matches(name)) {
            throw ClientSecretResolutionException(ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val prefixes = allowedPrefixes.map { it.trim() }.filter { it.isNotBlank() }
        if (prefixes.none(name::startsWith)) {
            throw ClientSecretResolutionException(ClientSecretResolutionStatus.POLICY_DENIED)
        }
    }

    companion object {
        private const val ENV_PREFIX = "env:"
        private const val PROPERTY_PREFIX = "property:"
        private val ENV_NAME_PATTERN = Regex("^[A-Za-z_][A-Za-z0-9_]*$")
        private val PROPERTY_NAME_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_.-]*$")
    }
}
