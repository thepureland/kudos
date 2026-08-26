package io.kudos.ms.auth.core.provider.management.service.impl

import io.kudos.ms.auth.common.provider.enums.ExternalJitPolicyEnum
import io.kudos.ms.auth.common.provider.enums.ExternalLinkPolicyEnum
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderCreateCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderManagementException
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderSetActiveCommand
import io.kudos.ms.auth.core.provider.management.model.IdentityProviderUpdateCommand
import io.kudos.ms.auth.core.provider.management.model.ManagedIdentityProvider
import io.kudos.ms.auth.core.provider.management.model.ManagedProviderTemplate
import io.kudos.ms.auth.core.provider.management.service.iservice.IIdentityProviderManagementService
import io.kudos.ms.auth.core.provider.model.po.AuthIdentityProvider
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import io.kudos.base.query.Criteria
import org.springframework.dao.DataIntegrityViolationException
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.URI
import java.time.LocalDateTime
import java.util.Locale

@Service
@Transactional
open class IdentityProviderManagementService(
    private val providerDao: AuthIdentityProviderDao,
    private val templateDao: AuthProviderTemplateDao,
) : IIdentityProviderManagementService {

    @Transactional(readOnly = true)
    override fun listTemplates(): List<ManagedProviderTemplate> =
        templateDao.search(null as Criteria?).asSequence()
        .filter { it.active }
        .map { it.toManaged() }
        .sortedBy { it.code }
        .toList()

    @Transactional(readOnly = true)
    override fun list(tenantId: String): List<ManagedIdentityProvider> {
        val normalizedTenantId = required(tenantId, 36, "EXTERNAL_PROVIDER_TENANT_REQUIRED")
        return providerDao.findByTenantId(normalizedTenantId).map { it.toManaged(requireTemplate(it.templateId)) }
            .sortedBy { it.displayName.lowercase(Locale.ROOT) }
    }

    @Transactional(readOnly = true)
    override fun get(providerId: String, tenantId: String): ManagedIdentityProvider {
        val provider = requireProvider(providerId, tenantId)
        return provider.toManaged(requireTemplate(provider.templateId))
    }

    override fun create(command: IdentityProviderCreateCommand): ManagedIdentityProvider {
        val tenantId = required(command.tenantId, 36, "EXTERNAL_PROVIDER_TENANT_REQUIRED")
        val actor = required(command.actorUserId, 36, "EXTERNAL_PROVIDER_ACTOR_REQUIRED")
        val reason = reason(command.operationReason)
        val template = requireTemplate(command.templateId)
        val code = normalizeCode(command.code)
        if (providerDao.findByTenantIdAndCode(tenantId, code) != null) fail("EXTERNAL_PROVIDER_CODE_EXISTS")
        val validated = validateConfiguration(
            template = template,
            displayName = command.displayName,
            issuer = command.issuer,
            clientId = command.clientId,
            clientSecretRef = command.clientSecretRef,
            scopes = command.scopes,
            jitPolicy = command.jitPolicy,
            linkPolicy = command.linkPolicy,
        )
        val now = LocalDateTime.now()
        val provider = AuthIdentityProvider {
            this.tenantId = tenantId
            templateId = template.id
            this.code = code
            displayName = validated.displayName
            issuer = validated.issuer
            clientId = validated.clientId
            clientSecretRef = validated.clientSecretRef
            scopes = validated.scopes.takeIf { it.isNotEmpty() }?.joinToString(" ")
            customConfig = null
            jitPolicy = validated.jitPolicy.name
            linkPolicy = validated.linkPolicy.name
            active = command.active
            createUserId = actor
            createReason = reason
            createTime = now
            updateUserId = actor
            updateReason = reason
            updateTime = now
        }
        try {
            provider.id = providerDao.insert(provider)
        } catch (e: DataIntegrityViolationException) {
            fail("EXTERNAL_PROVIDER_CODE_EXISTS", e)
        }
        return provider.toManaged(template)
    }

    override fun update(command: IdentityProviderUpdateCommand): ManagedIdentityProvider {
        val provider = requireProvider(command.providerId, command.tenantId)
        val actor = required(command.actorUserId, 36, "EXTERNAL_PROVIDER_ACTOR_REQUIRED")
        val reason = reason(command.operationReason)
        if (command.clearClientSecretRef && !command.clientSecretRef.isNullOrBlank()) {
            fail("EXTERNAL_PROVIDER_SECRET_REF_AMBIGUOUS")
        }
        val template = requireTemplate(provider.templateId)
        val secretRef = when {
            command.clearClientSecretRef -> null
            command.clientSecretRef != null -> command.clientSecretRef
            else -> provider.clientSecretRef
        }
        val validated = validateConfiguration(
            template = template,
            displayName = command.displayName,
            issuer = command.issuer,
            clientId = command.clientId,
            clientSecretRef = secretRef,
            scopes = command.scopes,
            jitPolicy = command.jitPolicy,
            linkPolicy = command.linkPolicy,
        )
        provider.displayName = validated.displayName
        provider.issuer = validated.issuer
        provider.clientId = validated.clientId
        provider.clientSecretRef = validated.clientSecretRef
        provider.scopes = validated.scopes.takeIf { it.isNotEmpty() }?.joinToString(" ")
        provider.jitPolicy = validated.jitPolicy.name
        provider.linkPolicy = validated.linkPolicy.name
        provider.updateUserId = actor
        provider.updateReason = reason
        provider.updateTime = LocalDateTime.now()
        if (!providerDao.update(provider)) fail("EXTERNAL_PROVIDER_UPDATE_FAILED")
        return provider.toManaged(template)
    }

    override fun setActive(command: IdentityProviderSetActiveCommand): ManagedIdentityProvider {
        val provider = requireProvider(command.providerId, command.tenantId)
        val actor = required(command.actorUserId, 36, "EXTERNAL_PROVIDER_ACTOR_REQUIRED")
        val reason = reason(command.operationReason)
        val template = requireTemplate(provider.templateId)
        if (command.active) {
            validateConfiguration(
                template, provider.displayName, provider.issuer, provider.clientId, provider.clientSecretRef,
                parseScopes(provider.scopes), provider.jitPolicy, provider.linkPolicy,
            )
        }
        provider.active = command.active
        provider.updateUserId = actor
        provider.updateReason = reason
        provider.updateTime = LocalDateTime.now()
        if (!providerDao.update(provider)) fail("EXTERNAL_PROVIDER_UPDATE_FAILED")
        return provider.toManaged(template)
    }

    private fun validateConfiguration(
        template: AuthProviderTemplate,
        displayName: String,
        issuer: String?,
        clientId: String,
        clientSecretRef: String?,
        scopes: Collection<String>,
        jitPolicy: String,
        linkPolicy: String,
    ): ValidatedConfiguration {
        val normalizedIssuer = issuer?.trim()?.takeIf { it.isNotBlank() }?.also(::validateIssuer)
        val normalizedScopes = normalizeScopes(scopes)
        val effectiveScopes = if (normalizedScopes.isEmpty()) parseScopes(template.defaultScopes) else normalizedScopes
        if (template.protocol.equals("OIDC", true) && "openid" !in effectiveScopes) {
            fail("EXTERNAL_PROVIDER_OIDC_OPENID_SCOPE_REQUIRED")
        }
        if (template.protocol.equals("OIDC", true) && normalizedIssuer == null && template.issuer == null &&
            template.authorizationUri == null
        ) fail("EXTERNAL_PROVIDER_ISSUER_REQUIRED")
        val normalizedJitPolicy = enumValue<ExternalJitPolicyEnum>(jitPolicy, "EXTERNAL_PROVIDER_JIT_POLICY_INVALID")
        val normalizedLinkPolicy = enumValue<ExternalLinkPolicyEnum>(
            linkPolicy, "EXTERNAL_PROVIDER_LINK_POLICY_INVALID"
        )
        if (normalizedLinkPolicy == ExternalLinkPolicyEnum.MATCH_VERIFIED_EMAIL) {
            fail("EXTERNAL_PROVIDER_LINK_POLICY_NOT_SUPPORTED")
        }
        return ValidatedConfiguration(
            displayName = required(displayName, 128, "EXTERNAL_PROVIDER_DISPLAY_NAME_INVALID"),
            issuer = normalizedIssuer,
            clientId = required(clientId, 256, "EXTERNAL_PROVIDER_CLIENT_ID_INVALID"),
            clientSecretRef = normalizeSecretRef(clientSecretRef),
            scopes = normalizedScopes,
            jitPolicy = normalizedJitPolicy,
            linkPolicy = normalizedLinkPolicy,
        )
    }

    private fun validateIssuer(value: String) {
        if (value.length > 512) fail("EXTERNAL_PROVIDER_ISSUER_INVALID")
        val uri = runCatching { URI(value) }.getOrElse { fail("EXTERNAL_PROVIDER_ISSUER_INVALID", it) }
        if (!uri.scheme.equals("https", true) || uri.host.isNullOrBlank() || uri.userInfo != null ||
            uri.fragment != null
        ) fail("EXTERNAL_PROVIDER_ISSUER_INVALID")
    }

    private fun normalizeSecretRef(value: String?): String? {
        val reference = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (reference.length > 512 || !SECRET_REF_PATTERN.matches(reference)) {
            fail("EXTERNAL_PROVIDER_SECRET_REF_INVALID")
        }
        return reference
    }

    private fun normalizeScopes(values: Collection<String>): List<String> {
        val normalized = values.flatMap { it.split(Regex("[\\s,]+")) }
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .distinct()
        if (normalized.any { it.length > 128 } || normalized.joinToString(" ").length > 512) {
            fail("EXTERNAL_PROVIDER_SCOPES_INVALID")
        }
        return normalized
    }

    private fun parseScopes(value: String?): List<String> =
        value.orEmpty().split(Regex("[\\s,]+")).filter { it.isNotBlank() }.distinct()

    private fun requireProvider(providerId: String, tenantId: String): AuthIdentityProvider {
        val normalizedTenantId = required(tenantId, 36, "EXTERNAL_PROVIDER_TENANT_REQUIRED")
        val provider = providerDao.get(required(providerId, 36, "EXTERNAL_PROVIDER_ID_REQUIRED"))
            ?: fail("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        if (provider.tenantId != normalizedTenantId) fail("EXTERNAL_PROVIDER_TENANT_MISMATCH")
        return provider
    }

    private fun requireTemplate(templateId: String): AuthProviderTemplate =
        templateDao.get(required(templateId, 36, "EXTERNAL_PROVIDER_TEMPLATE_REQUIRED"))
            ?.takeIf { it.active }
            ?: fail("EXTERNAL_PROVIDER_TEMPLATE_NOT_AVAILABLE")

    private fun normalizeCode(value: String): String {
        val code = value.trim().lowercase(Locale.ROOT)
        if (!CODE_PATTERN.matches(code)) fail("EXTERNAL_PROVIDER_CODE_INVALID")
        return code
    }

    private fun reason(value: String): String {
        val normalized = value.trim()
        if (normalized.isBlank() || normalized.length > 512) fail("EXTERNAL_PROVIDER_REASON_INVALID")
        return normalized
    }

    private fun required(value: String, maxLength: Int, errorCode: String): String {
        val normalized = value.trim()
        if (normalized.isBlank() || normalized.length > maxLength) fail(errorCode)
        return normalized
    }

    private inline fun <reified E : Enum<E>> enumValue(value: String, errorCode: String): E = runCatching {
        enumValueOf<E>(value.trim().uppercase(Locale.ROOT))
    }.getOrElse { fail(errorCode, it) }

    private fun AuthProviderTemplate.toManaged() = ManagedProviderTemplate(
        id = id,
        code = code,
        protocol = protocol,
        issuer = issuer,
        defaultScopes = parseScopes(defaultScopes),
        logoUri = logoUri,
    )

    private fun AuthIdentityProvider.toManaged(template: AuthProviderTemplate) = ManagedIdentityProvider(
        id = id,
        templateId = templateId,
        templateCode = template.code,
        protocol = template.protocol,
        code = code,
        displayName = displayName,
        issuer = issuer,
        clientId = clientId,
        clientSecretConfigured = !clientSecretRef.isNullOrBlank(),
        scopes = parseScopes(scopes),
        effectiveScopes = parseScopes(scopes ?: template.defaultScopes),
        jitPolicy = jitPolicy,
        linkPolicy = linkPolicy,
        active = active,
    )

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw IdentityProviderManagementException(errorCode, cause)

    private data class ValidatedConfiguration(
        val displayName: String,
        val issuer: String?,
        val clientId: String,
        val clientSecretRef: String?,
        val scopes: List<String>,
        val jitPolicy: ExternalJitPolicyEnum,
        val linkPolicy: ExternalLinkPolicyEnum,
    )

    private companion object {
        val CODE_PATTERN = Regex("^[a-z][a-z0-9_-]{0,31}$")
        val SECRET_REF_PATTERN = Regex("^[a-z][a-z0-9+.-]{0,31}:[^\\s]+$")
    }
}
