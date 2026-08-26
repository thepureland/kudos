package io.kudos.ms.auth.core.provider.claim.service.impl

import io.kudos.ms.auth.core.provider.claim.dao.AuthIdentityProviderClaimMappingDao
import io.kudos.ms.auth.core.provider.claim.model.EffectiveIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingException
import io.kudos.ms.auth.core.provider.claim.model.IdentityProviderClaimMappingSaveCommand
import io.kudos.ms.auth.core.provider.claim.model.po.AuthIdentityProviderClaimMapping
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.model.po.AuthProviderTemplate
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime

@Service
@Transactional
open class IdentityProviderClaimMappingService(
    private val dao: AuthIdentityProviderClaimMappingDao,
    private val providerDao: AuthIdentityProviderDao,
    private val templateDao: AuthProviderTemplateDao,
) : IIdentityProviderClaimMappingService {

    @Transactional(readOnly = true)
    override fun getEffective(providerId: String, tenantId: String): EffectiveIdentityProviderClaimMapping {
        val scope = requireScope(providerId, tenantId)
        return dao.get(scope.providerId)?.toEffective()
            ?: defaults(scope.providerId, scope.template)
    }

    override fun save(command: IdentityProviderClaimMappingSaveCommand): EffectiveIdentityProviderClaimMapping {
        val scope = requireScope(command.providerId, command.tenantId)
        val actor = command.actorUserId.trim()
        if (actor.isBlank() || actor.length > 36) fail("EXTERNAL_CLAIM_MAPPING_ACTOR_REQUIRED")
        val reason = command.operationReason.trim()
        if (reason.isBlank() || reason.length > 512) fail("EXTERNAL_CLAIM_MAPPING_REASON_INVALID")

        val subject = normalize(command.subjectClaims, required = true)
        if (scope.template.protocol.equals("OIDC", ignoreCase = true) && subject != listOf(OIDC_SUBJECT_CLAIM)) {
            fail("EXTERNAL_CLAIM_MAPPING_OIDC_SUBJECT_IMMUTABLE")
        }
        val username = normalize(command.usernameClaims)
        val displayName = normalize(command.displayNameClaims)
        val email = normalize(command.emailClaims)
        val emailVerified = normalize(command.emailVerifiedClaims)
        val phone = normalize(command.phoneClaims)
        val phoneVerified = normalize(command.phoneVerifiedClaims)
        val avatar = normalize(command.avatarClaims)
        val locale = normalize(command.localeClaims)
        val unionId = normalize(command.unionIdClaims)
        val now = LocalDateTime.now()
        val existing = dao.get(scope.providerId)
        val mapping = existing ?: AuthIdentityProviderClaimMapping {
            id = scope.providerId
            tenantId = scope.tenantId
            createUserId = actor
            createReason = reason
            createTime = now
        }
        mapping.tenantId = scope.tenantId
        mapping.subjectClaims = subject.joinToString(",")
        mapping.usernameClaims = username.csv()
        mapping.displayNameClaims = displayName.csv()
        mapping.emailClaims = email.csv()
        mapping.emailVerifiedClaims = emailVerified.csv()
        mapping.phoneClaims = phone.csv()
        mapping.phoneVerifiedClaims = phoneVerified.csv()
        mapping.avatarClaims = avatar.csv()
        mapping.localeClaims = locale.csv()
        mapping.unionIdClaims = unionId.csv()
        mapping.updateUserId = actor
        mapping.updateReason = reason
        mapping.updateTime = now
        if (existing == null) {
            dao.insert(mapping)
        } else if (!dao.update(mapping)) {
            fail("EXTERNAL_CLAIM_MAPPING_UPDATE_FAILED")
        }
        return mapping.toEffective()
    }

    private fun requireScope(providerId: String, tenantId: String): ProviderScope {
        val normalizedProviderId = required(providerId, "EXTERNAL_PROVIDER_ID_REQUIRED")
        val normalizedTenantId = required(tenantId, "EXTERNAL_PROVIDER_TENANT_REQUIRED")
        val provider = providerDao.get(normalizedProviderId) ?: fail("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        if (provider.tenantId != normalizedTenantId) fail("EXTERNAL_PROVIDER_TENANT_MISMATCH")
        val template = templateDao.get(provider.templateId)?.takeIf { it.active }
            ?: fail("EXTERNAL_PROVIDER_TEMPLATE_NOT_AVAILABLE")
        return ProviderScope(normalizedProviderId, normalizedTenantId, template)
    }

    private fun required(value: String, errorCode: String): String = value.trim().also {
        if (it.isBlank() || it.length > 36) fail(errorCode)
    }

    private fun normalize(values: Collection<String>, required: Boolean = false): List<String> {
        val normalized = values.map { it.trim() }.filter { it.isNotBlank() }.distinct()
        if ((required && normalized.isEmpty()) || normalized.size > MAX_PATHS ||
            normalized.any { it.length > MAX_PATH_LENGTH || !CLAIM_PATH_PATTERN.matches(it) } ||
            normalized.joinToString(",").length > MAX_COLUMN_LENGTH
        ) fail("EXTERNAL_CLAIM_MAPPING_PATH_INVALID")
        return normalized
    }

    private fun defaults(providerId: String, template: AuthProviderTemplate) =
        EffectiveIdentityProviderClaimMapping(
            providerId = providerId,
            subjectClaims = if (template.protocol.equals("OIDC", ignoreCase = true)) {
                listOf(OIDC_SUBJECT_CLAIM)
            } else {
                listOf(template.subjectClaim)
            },
        )

    private fun AuthIdentityProviderClaimMapping.toEffective() = EffectiveIdentityProviderClaimMapping(
        providerId = id,
        subjectClaims = subjectClaims.paths(),
        usernameClaims = usernameClaims.paths(),
        displayNameClaims = displayNameClaims.paths(),
        emailClaims = emailClaims.paths(),
        emailVerifiedClaims = emailVerifiedClaims.paths(),
        phoneClaims = phoneClaims.paths(),
        phoneVerifiedClaims = phoneVerifiedClaims.paths(),
        avatarClaims = avatarClaims.paths(),
        localeClaims = localeClaims.paths(),
        unionIdClaims = unionIdClaims.paths(),
        configured = true,
    )

    private fun String?.paths(): List<String> = this?.split(',')?.filter { it.isNotBlank() } ?: emptyList()
    private fun List<String>.csv(): String? = takeIf { it.isNotEmpty() }?.joinToString(",")

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw IdentityProviderClaimMappingException(errorCode, cause)

    private data class ProviderScope(
        val providerId: String,
        val tenantId: String,
        val template: AuthProviderTemplate,
    )

    private companion object {
        const val MAX_PATHS = 8
        const val MAX_PATH_LENGTH = 128
        const val MAX_COLUMN_LENGTH = 512
        const val OIDC_SUBJECT_CLAIM = "sub"
        val CLAIM_PATH_PATTERN = Regex("^[A-Za-z0-9_-]+(?:\\.[A-Za-z0-9_-]+)*$")
    }
}
