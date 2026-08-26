package io.kudos.ms.auth.secret.azure

import com.azure.security.keyvault.secrets.SecretClient
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.IClientSecretResolver
import io.kudos.ms.auth.secret.azure.init.AzureKeyVaultClientSecretProperties
import tools.jackson.databind.json.JsonMapper
import java.net.URI

/** Reads an Azure Key Vault secret without logging its vault, name, value, or API error. */
open class AzureKeyVaultClientSecretResolver(
    private val clients: List<SecretClient>,
    private val properties: AzureKeyVaultClientSecretProperties,
) : IClientSecretResolver {

    private val jsonMapper = JsonMapper.builder().build()

    override fun supports(reference: String): Boolean = reference.startsWith(REFERENCE_PREFIX)

    override fun resolve(reference: String): String? {
        val target = parseReference(reference)
        val version = configuredVersion()
        val client = selectClient(target.vaultUrl)
        val secret = if (version == null) {
            client.getSecret(target.secretName)
        } else {
            client.getSecret(target.secretName, version)
        } ?: fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        if (!secret.name.equals(target.secretName, ignoreCase = true)) {
            fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        val value = secret.value?.takeIf { it.isNotBlank() } ?: return null
        if (value.toByteArray(Charsets.UTF_8).size > MAX_SECRET_BYTES) {
            fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        val field = target.jsonField ?: return value
        val root = runCatching { jsonMapper.readTree(value) }
            .getOrElse { fail(ClientSecretResolutionStatus.RESOLVER_ERROR) }
        if (!root.isObject) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        val fieldValue = root.get(field) ?: return null
        if (!fieldValue.isString) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return fieldValue.stringValue().takeIf { it.isNotBlank() }
    }

    private fun parseReference(reference: String): SecretTarget {
        if (!supports(reference) || reference.length > MAX_REFERENCE_LENGTH) {
            fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val raw = reference.removePrefix(REFERENCE_PREFIX)
        if (raw.count { it == FIELD_SEPARATOR } > 1) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val resource = raw.substringBefore(FIELD_SEPARATOR)
        if (resource.count { it == '/' } != 1) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val vaultAlias = resource.substringBefore('/')
        val secretName = resource.substringAfter('/')
        val jsonField = if (FIELD_SEPARATOR in raw) {
            raw.substringAfter(FIELD_SEPARATOR).takeIf(SAFE_FIELD_PATTERN::matches)
                ?: fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        } else null
        if (!SAFE_VAULT_ALIAS_PATTERN.matches(vaultAlias) || !SAFE_SECRET_NAME_PATTERN.matches(secretName)) {
            fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val vaultUrl = configuredVaultUrls()[vaultAlias]
            ?: fail(ClientSecretResolutionStatus.POLICY_DENIED)
        val prefixes = properties.allowedSecretNamePrefixes.map {
            configured(it, SAFE_SECRET_PREFIX_PATTERN)
        }
        if (prefixes.none(secretName::startsWith)) fail(ClientSecretResolutionStatus.POLICY_DENIED)
        return SecretTarget(vaultUrl, secretName, jsonField)
    }

    private fun configuredVaultUrls(): Map<String, String> {
        val configured = LinkedHashMap<String, String>()
        properties.vaultUrls.forEach { (rawAlias, rawUrl) ->
            val alias = configured(rawAlias, SAFE_VAULT_ALIAS_PATTERN)
            val previous = configured.put(alias, normalizeVaultUrl(rawUrl))
            if (previous != null) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        return configured
    }

    private fun selectClient(vaultUrl: String): SecretClient {
        val matches = clients.filter { client ->
            runCatching { normalizeVaultUrl(client.vaultUrl) }
                .getOrElse { fail(ClientSecretResolutionStatus.RESOLVER_ERROR) } == vaultUrl
        }
        if (matches.size != 1) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return matches.single()
    }

    private fun normalizeVaultUrl(value: String): String {
        val uri = runCatching { URI(value.trim()) }
            .getOrElse { fail(ClientSecretResolutionStatus.RESOLVER_ERROR) }
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.host.isNullOrBlank() ||
            uri.userInfo != null || uri.port != -1 || uri.query != null || uri.fragment != null ||
            (uri.path.isNotEmpty() && uri.path != "/")) {
            fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        return "https://${uri.host.lowercase()}"
    }

    private fun configuredVersion(): String? {
        val version = properties.version.trim()
        if (version.isEmpty()) return null
        if (!SAFE_VERSION_PATTERN.matches(version)) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return version
    }

    private fun configured(value: String, pattern: Regex): String {
        val normalized = value.trim()
        if (!pattern.matches(normalized)) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return normalized
    }

    private fun fail(status: ClientSecretResolutionStatus): Nothing =
        throw ClientSecretResolutionException(status)

    private data class SecretTarget(val vaultUrl: String, val secretName: String, val jsonField: String?)

    private companion object {
        const val REFERENCE_PREFIX = "azure-kv:"
        const val FIELD_SEPARATOR = '#'
        const val MAX_REFERENCE_LENGTH = 512
        const val MAX_SECRET_BYTES = 65_536
        val SAFE_VAULT_ALIAS_PATTERN = Regex("^[a-z][a-z0-9-]{0,62}$")
        val SAFE_SECRET_NAME_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9-]{0,126}$")
        val SAFE_SECRET_PREFIX_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9-]{0,126}$")
        val SAFE_FIELD_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$")
        val SAFE_VERSION_PATTERN = Regex("^[A-Fa-f0-9]{32}$")
    }
}
