package io.kudos.ms.auth.secret.vault

import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.IClientSecretResolver
import io.kudos.ms.auth.secret.vault.init.VaultClientSecretProperties
import org.springframework.beans.factory.ObjectProvider
import org.springframework.vault.core.VaultOperations

/** Reads one string from a server-pinned HashiCorp Vault KV mount without logging location or value. */
open class VaultClientSecretResolver(
    private val vaultOperationsProvider: ObjectProvider<VaultOperations>,
    private val properties: VaultClientSecretProperties,
) : IClientSecretResolver {

    override fun supports(reference: String): Boolean = reference.startsWith(REFERENCE_PREFIX)

    override fun resolve(reference: String): String? {
        val target = parseReference(reference)
        val operations = vaultOperationsProvider.getIfAvailable()
            ?: fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        val response = operations.read(readPath(target.path)) ?: return null
        val data = when (properties.backendVersion) {
            1 -> response.data
            2 -> response.data?.get(KV2_DATA_KEY) as? Map<*, *> ?: return null
            else -> fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        } ?: return null
        val value = data[target.key] ?: return null
        if (value !is String) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return value.takeIf { it.isNotBlank() }
    }

    private fun parseReference(reference: String): SecretTarget {
        if (!supports(reference) || reference.length > MAX_REFERENCE_LENGTH) {
            fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val target = reference.removePrefix(REFERENCE_PREFIX)
        if (target.count { it == FIELD_SEPARATOR } > 1) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val path = target.substringBefore(FIELD_SEPARATOR)
        val explicitKey = target.substringAfter(FIELD_SEPARATOR, missingDelimiterValue = "")
        if (!safePath(path)) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val key = if (FIELD_SEPARATOR in target) {
            explicitKey.takeIf { SAFE_KEY_PATTERN.matches(it) }
                ?: fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        } else {
            properties.defaultKey.takeIf { SAFE_KEY_PATTERN.matches(it) }
                ?: fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        val allowedPrefixes = properties.allowedPathPrefixes.map(::configurationPath)
        if (allowedPrefixes.none { path == it || path.startsWith("$it/") }) {
            fail(ClientSecretResolutionStatus.POLICY_DENIED)
        }
        return SecretTarget(path, key)
    }

    private fun readPath(path: String): String {
        val mount = configurationPath(properties.mount)
        return when (properties.backendVersion) {
            1 -> "$mount/$path"
            2 -> "$mount/data/$path"
            else -> fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
    }

    private fun configurationPath(value: String): String {
        val normalized = value.trim().trim('/')
        if (!safePath(normalized)) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return normalized
    }

    private fun safePath(value: String): Boolean =
        SAFE_PATH_PATTERN.matches(value) && value.split('/').none { it == "." || it == ".." }

    private fun fail(status: ClientSecretResolutionStatus): Nothing =
        throw ClientSecretResolutionException(status)

    private data class SecretTarget(val path: String, val key: String)

    private companion object {
        const val REFERENCE_PREFIX = "vault:"
        const val FIELD_SEPARATOR = '#'
        const val KV2_DATA_KEY = "data"
        const val MAX_REFERENCE_LENGTH = 512
        val SAFE_PATH_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._-]*(/[A-Za-z0-9][A-Za-z0-9._-]*)*$")
        val SAFE_KEY_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9._-]{0,127}$")
    }
}
