package io.kudos.ms.auth.secret.google

import com.google.cloud.secretmanager.v1.AccessSecretVersionRequest
import com.google.cloud.secretmanager.v1.SecretManagerServiceClient
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.IClientSecretResolver
import io.kudos.ms.auth.secret.google.init.GoogleSecretManagerClientSecretProperties
import org.springframework.beans.factory.ObjectProvider
import tools.jackson.databind.json.JsonMapper
import java.util.zip.CRC32C

/** Reads a Google Secret Manager UTF-8 payload without logging its resource, value, or API error. */
open class GoogleSecretManagerClientSecretResolver(
    private val clientProvider: ObjectProvider<SecretManagerServiceClient>,
    private val properties: GoogleSecretManagerClientSecretProperties,
) : IClientSecretResolver {

    private val jsonMapper = JsonMapper.builder().build()

    override fun supports(reference: String): Boolean = reference.startsWith(REFERENCE_PREFIX)

    override fun resolve(reference: String): String? {
        val target = parseReference(reference)
        val client = clientProvider.getIfAvailable() ?: fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        val response = client.accessSecretVersion(
            AccessSecretVersionRequest.newBuilder()
                .setName("projects/${target.projectId}/secrets/${target.secretId}/versions/${version()}")
                .build()
        )
        if (!response.hasPayload()) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        val payload = response.payload
        val data = payload.data
        if (data.size() > MAX_SECRET_BYTES || !data.isValidUtf8) {
            fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        if (payload.hasDataCrc32C()) {
            val checksum = CRC32C().apply { update(data.toByteArray()) }.value
            if (checksum != payload.dataCrc32C) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        val secret = data.toStringUtf8().takeIf { it.isNotBlank() } ?: return null
        val field = target.jsonField ?: return secret
        val root = runCatching { jsonMapper.readTree(secret) }
            .getOrElse { fail(ClientSecretResolutionStatus.RESOLVER_ERROR) }
        if (!root.isObject) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        val value = root.get(field) ?: return null
        if (!value.isString) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return value.stringValue().takeIf { it.isNotBlank() }
    }

    private fun parseReference(reference: String): SecretTarget {
        if (!supports(reference) || reference.length > MAX_REFERENCE_LENGTH) {
            fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val raw = reference.removePrefix(REFERENCE_PREFIX)
        if (raw.count { it == FIELD_SEPARATOR } > 1) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val resource = raw.substringBefore(FIELD_SEPARATOR)
        if (resource.count { it == '/' } != 1) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val projectId = resource.substringBefore('/')
        val secretId = resource.substringAfter('/')
        val jsonField = if (FIELD_SEPARATOR in raw) {
            raw.substringAfter(FIELD_SEPARATOR).takeIf(SAFE_FIELD_PATTERN::matches)
                ?: fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        } else null
        if (!SAFE_PROJECT_ID_PATTERN.matches(projectId) || !SAFE_SECRET_ID_PATTERN.matches(secretId)) {
            fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val projects = properties.allowedProjectIds.map { configured(it, SAFE_PROJECT_ID_PATTERN) }
        if (projectId !in projects) fail(ClientSecretResolutionStatus.POLICY_DENIED)
        val prefixes = properties.allowedSecretIdPrefixes.map { configured(it, SAFE_SECRET_PREFIX_PATTERN) }
        if (prefixes.none(secretId::startsWith)) fail(ClientSecretResolutionStatus.POLICY_DENIED)
        return SecretTarget(projectId, secretId, jsonField)
    }

    private fun configured(value: String, pattern: Regex): String {
        val normalized = value.trim()
        if (!pattern.matches(normalized)) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        return normalized
    }

    private fun version(): String {
        val value = properties.version.trim()
        if (value != LATEST_VERSION && !POSITIVE_VERSION_PATTERN.matches(value)) {
            fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        return value
    }

    private fun fail(status: ClientSecretResolutionStatus): Nothing =
        throw ClientSecretResolutionException(status)

    private data class SecretTarget(val projectId: String, val secretId: String, val jsonField: String?)

    private companion object {
        const val REFERENCE_PREFIX = "gcp-sm:"
        const val FIELD_SEPARATOR = '#'
        const val LATEST_VERSION = "latest"
        const val MAX_REFERENCE_LENGTH = 512
        const val MAX_SECRET_BYTES = 65_536
        val SAFE_PROJECT_ID_PATTERN = Regex("^(?:[a-z][a-z0-9-]{4,28}[a-z0-9]|[0-9]{6,20})$")
        val SAFE_SECRET_ID_PATTERN = Regex("^[A-Za-z0-9_-]{1,255}$")
        val SAFE_SECRET_PREFIX_PATTERN = Regex("^[A-Za-z0-9_-]{1,255}$")
        val SAFE_FIELD_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$")
        val POSITIVE_VERSION_PATTERN = Regex("^[1-9][0-9]{0,18}$")
    }
}
