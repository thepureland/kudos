package io.kudos.ms.auth.secret.aws

import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionException
import io.kudos.ms.auth.provider.oauth2.secret.ClientSecretResolutionStatus
import io.kudos.ms.auth.provider.oauth2.secret.IClientSecretResolver
import io.kudos.ms.auth.secret.aws.init.AwsSecretsManagerClientSecretProperties
import org.springframework.beans.factory.ObjectProvider
import software.amazon.awssdk.services.secretsmanager.SecretsManagerClient
import software.amazon.awssdk.services.secretsmanager.model.GetSecretValueRequest
import tools.jackson.databind.json.JsonMapper

/** Reads an AWS Secrets Manager SecretString without logging its id, ARN, value, or SDK error. */
open class AwsSecretsManagerClientSecretResolver(
    private val clientProvider: ObjectProvider<SecretsManagerClient>,
    private val properties: AwsSecretsManagerClientSecretProperties,
) : IClientSecretResolver {

    private val jsonMapper = JsonMapper.builder().build()

    override fun supports(reference: String): Boolean = reference.startsWith(REFERENCE_PREFIX)

    override fun resolve(reference: String): String? {
        val target = parseReference(reference)
        val client = clientProvider.getIfAvailable() ?: fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        val response = client.getSecretValue(
            GetSecretValueRequest.builder()
                .secretId(target.secretId)
                .versionStage(versionStage())
                .build()
        )
        if (response.secretString() == null) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        val secret = response.secretString().takeIf { it.isNotBlank() } ?: return null
        if (secret.length > MAX_SECRET_STRING_LENGTH) fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
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
        val target = reference.removePrefix(REFERENCE_PREFIX)
        if (target.count { it == FIELD_SEPARATOR } > 1) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val secretId = target.substringBefore(FIELD_SEPARATOR)
        val jsonField = if (FIELD_SEPARATOR in target) {
            target.substringAfter(FIELD_SEPARATOR).takeIf(SAFE_FIELD_PATTERN::matches)
                ?: fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        } else null
        if (secretId.startsWith(ARN_PREFIX)) validateArn(secretId) else validateName(secretId)
        return SecretTarget(secretId, jsonField)
    }

    private fun validateName(secretId: String) {
        if (!SAFE_NAME_PATTERN.matches(secretId) || unsafeSegments(secretId)) {
            fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        }
        val allowedPrefixes = properties.allowedSecretIdPrefixes.map(::configurationName)
        if (allowedPrefixes.none { secretId == it || secretId.startsWith("$it/") }) {
            fail(ClientSecretResolutionStatus.POLICY_DENIED)
        }
    }

    private fun validateArn(secretId: String) {
        if (!SAFE_ARN_PATTERN.matches(secretId)) fail(ClientSecretResolutionStatus.INVALID_REFERENCE)
        val prefixes = properties.allowedArnPrefixes.map { it.trim() }
        if (!properties.allowArns || prefixes.isEmpty()) fail(ClientSecretResolutionStatus.POLICY_DENIED)
        if (prefixes.any { !SAFE_ARN_PREFIX_PATTERN.matches(it) }) {
            fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        if (prefixes.none { secretId == it || it.endsWith('/') && secretId.startsWith(it) }) {
            fail(ClientSecretResolutionStatus.POLICY_DENIED)
        }
    }

    private fun configurationName(value: String): String {
        val normalized = value.trim().trim('/')
        if (!SAFE_NAME_PATTERN.matches(normalized) || unsafeSegments(normalized)) {
            fail(ClientSecretResolutionStatus.RESOLVER_ERROR)
        }
        return normalized
    }

    private fun versionStage(): String = properties.versionStage.trim().takeIf(SAFE_STAGE_PATTERN::matches)
        ?: fail(ClientSecretResolutionStatus.RESOLVER_ERROR)

    private fun unsafeSegments(value: String): Boolean =
        value.startsWith('/') || value.contains("//") || value.split('/').any { it == "." || it == ".." }

    private fun fail(status: ClientSecretResolutionStatus): Nothing =
        throw ClientSecretResolutionException(status)

    private data class SecretTarget(val secretId: String, val jsonField: String?)

    private companion object {
        const val REFERENCE_PREFIX = "aws-sm:"
        const val ARN_PREFIX = "arn:"
        const val FIELD_SEPARATOR = '#'
        const val MAX_REFERENCE_LENGTH = 512
        const val MAX_SECRET_STRING_LENGTH = 65_536
        val SAFE_NAME_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9/_+=.@-]{0,511}$")
        val SAFE_FIELD_PATTERN = Regex("^[A-Za-z0-9][A-Za-z0-9_.-]{0,127}$")
        val SAFE_STAGE_PATTERN = Regex("^[A-Za-z0-9/_+=.@-]{1,256}$")
        val SAFE_ARN_PATTERN = Regex(
            "^arn:[a-z0-9-]+:secretsmanager:[a-z0-9-]+:[0-9]{12}:secret:[A-Za-z0-9/_+=.@-]+$"
        )
        val SAFE_ARN_PREFIX_PATTERN = Regex(
            "^arn:[a-z0-9-]+:secretsmanager:[a-z0-9-]+:[0-9]{12}:secret:[A-Za-z0-9/_+=.@-]*$"
        )
    }
}
