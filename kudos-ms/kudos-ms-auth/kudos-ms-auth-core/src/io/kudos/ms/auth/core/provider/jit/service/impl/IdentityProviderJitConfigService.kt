package io.kudos.ms.auth.core.provider.jit.service.impl

import io.kudos.ms.auth.common.provider.enums.ExternalJitPolicyEnum
import io.kudos.ms.auth.common.provider.enums.ExternalJitUsernameStrategyEnum
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.jit.dao.AuthIdentityProviderJitConfigDao
import io.kudos.ms.auth.core.provider.jit.model.EffectiveIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigException
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigSaveCommand
import io.kudos.ms.auth.core.provider.jit.model.po.AuthIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.service.iservice.IIdentityProviderJitConfigService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.org.service.iservice.IUserOrgService
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.net.IDN
import java.time.LocalDateTime
import java.time.ZoneId
import java.util.Currency
import java.util.Locale

@Service
@Transactional
open class IdentityProviderJitConfigService(
    private val dao: AuthIdentityProviderJitConfigDao,
    private val identityProviderDao: AuthIdentityProviderDao,
    private val userOrgService: IUserOrgService,
    private val userAccountService: IUserAccountService,
) : IIdentityProviderJitConfigService {

    @Transactional(readOnly = true)
    override fun getEffective(providerId: String, tenantId: String): EffectiveIdentityProviderJitConfig {
        requireJitProvider(providerId, tenantId)
        return dao.get(providerId)?.toEffective()
            ?: EffectiveIdentityProviderJitConfig(providerId = providerId)
    }

    override fun save(command: IdentityProviderJitConfigSaveCommand): EffectiveIdentityProviderJitConfig {
        requireJitProvider(command.providerId, command.tenantId)
        if (command.actorUserId.isBlank()) fail("EXTERNAL_JIT_CONFIG_ACTOR_REQUIRED")
        val reason = command.operationReason.trim()
        if (reason.isBlank() || reason.length > 512) fail("EXTERNAL_JIT_CONFIG_REASON_INVALID")

        val strategy = runCatching {
            ExternalJitUsernameStrategyEnum.valueOf(command.usernameStrategy.trim().uppercase())
        }.getOrElse { fail("EXTERNAL_JIT_USERNAME_STRATEGY_INVALID", it) }
        val domains = normalizeDomains(command.allowedEmailDomains)
        val requireVerifiedEmail = command.requireVerifiedEmail || domains.isNotEmpty() ||
            strategy == ExternalJitUsernameStrategyEnum.EMAIL_LOCAL_PART_HASHED
        val orgId = command.defaultOrgId.normalized(MAX_ORG_ID_LENGTH)
        val supervisorId = command.defaultSupervisorId.normalized(36)
        val accountType = command.accountTypeDictCode.normalized(5)
        val accountStatus = command.accountStatusDictCode.normalized(5)
        val locale = normalizeLocale(command.defaultLocale)
        val timezone = normalizeTimezone(command.defaultTimezone)
        val currency = normalizeCurrency(command.defaultCurrency)

        orgId?.let {
            val org = userOrgService.getOrgRecord(it)
            if (org == null || org.active != true) fail("EXTERNAL_JIT_DEFAULT_ORG_NOT_AVAILABLE")
            if (org.tenantId != command.tenantId) fail("EXTERNAL_JIT_DEFAULT_ORG_TENANT_MISMATCH")
        }
        supervisorId?.let {
            val supervisor = userAccountService.get(it)
            if (supervisor == null || supervisor.active != true) fail("EXTERNAL_JIT_DEFAULT_SUPERVISOR_NOT_AVAILABLE")
            if (supervisor.tenantId != command.tenantId) {
                fail("EXTERNAL_JIT_DEFAULT_SUPERVISOR_TENANT_MISMATCH")
            }
        }

        val now = LocalDateTime.now()
        val existing = dao.get(command.providerId)
        val config = existing ?: AuthIdentityProviderJitConfig {
            id = command.providerId
            tenantId = command.tenantId
            createUserId = command.actorUserId
            createReason = reason
            createTime = now
        }
        config.tenantId = command.tenantId
        config.usernameStrategy = strategy.name
        config.requireVerifiedEmail = requireVerifiedEmail
        config.allowedEmailDomains = domains.takeIf { it.isNotEmpty() }?.joinToString(",")
        config.defaultOrgId = orgId
        config.defaultSupervisorId = supervisorId
        config.accountTypeDictCode = accountType
        config.accountStatusDictCode = accountStatus
        config.defaultLocale = locale
        config.defaultTimezone = timezone
        config.defaultCurrency = currency
        config.updateUserId = command.actorUserId
        config.updateReason = reason
        config.updateTime = now

        if (existing == null) {
            dao.insert(config)
        } else if (!dao.update(config)) {
            fail("EXTERNAL_JIT_CONFIG_UPDATE_FAILED")
        }
        return config.toEffective()
    }

    private fun requireJitProvider(providerId: String, tenantId: String) {
        val provider = identityProviderDao.get(providerId)
            ?: fail("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        if (provider.tenantId != tenantId) fail("EXTERNAL_PROVIDER_TENANT_MISMATCH")
        if (!provider.active) fail("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        val policy = runCatching { ExternalJitPolicyEnum.valueOf(provider.jitPolicy.uppercase()) }.getOrNull()
        if (policy != ExternalJitPolicyEnum.JIT_CREATE) fail("EXTERNAL_PROVIDER_NOT_JIT_CREATE")
    }

    private fun normalizeDomains(values: Collection<String>): List<String> {
        val normalized = values.map { raw ->
            val value = raw.trim().lowercase(Locale.ROOT).removePrefix("@")
            val wildcard = value.startsWith("*.")
            val domain = if (wildcard) value.removePrefix("*.") else value
            if (domain.isBlank() || domain.length > 253 || domain.contains(',')) {
                fail("EXTERNAL_JIT_EMAIL_DOMAIN_INVALID")
            }
            val ascii = runCatching { IDN.toASCII(domain, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT) }
                .getOrElse { fail("EXTERNAL_JIT_EMAIL_DOMAIN_INVALID", it) }
            if (!isValidDomain(ascii)) fail("EXTERNAL_JIT_EMAIL_DOMAIN_INVALID")
            if (wildcard) "*.$ascii" else ascii
        }.distinct().sorted()
        if (normalized.joinToString(",").length > MAX_DOMAIN_LIST_LENGTH) {
            fail("EXTERNAL_JIT_EMAIL_DOMAIN_LIST_TOO_LONG")
        }
        return normalized
    }

    private fun isValidDomain(value: String): Boolean =
        value.split('.').all { label ->
            label.isNotBlank() && label.length <= 63 &&
                label.first().isLetterOrDigit() && label.last().isLetterOrDigit() &&
                label.all { it.isLetterOrDigit() || it == '-' }
        }

    private fun normalizeLocale(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        val match = LOCALE_PATTERN.matchEntire(raw) ?: fail("EXTERNAL_JIT_DEFAULT_LOCALE_INVALID")
        val language = match.groupValues[1].lowercase(Locale.ROOT)
        val country = match.groupValues[2].takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)
        return if (country == null) language else "${language}_${country}"
    }

    private fun normalizeTimezone(value: String?): String? {
        val raw = value?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (raw.length > 64) fail("EXTERNAL_JIT_DEFAULT_TIMEZONE_INVALID")
        return runCatching { ZoneId.of(raw).id }
            .getOrElse { fail("EXTERNAL_JIT_DEFAULT_TIMEZONE_INVALID", it) }
    }

    private fun normalizeCurrency(value: String?): String? {
        val raw = value?.trim()?.uppercase(Locale.ROOT)?.takeIf { it.isNotBlank() } ?: return null
        return runCatching { Currency.getInstance(raw).currencyCode }
            .getOrElse { fail("EXTERNAL_JIT_DEFAULT_CURRENCY_INVALID", it) }
    }

    private fun String?.normalized(maxLength: Int): String? {
        val value = this?.trim()?.takeIf { it.isNotBlank() } ?: return null
        if (value.length > maxLength) fail("EXTERNAL_JIT_CONFIG_VALUE_TOO_LONG")
        return value
    }

    private fun AuthIdentityProviderJitConfig.toEffective() = EffectiveIdentityProviderJitConfig(
        providerId = id,
        usernameStrategy = runCatching { ExternalJitUsernameStrategyEnum.valueOf(usernameStrategy) }
            .getOrElse { fail("EXTERNAL_JIT_CONFIG_INVALID", it) },
        requireVerifiedEmail = requireVerifiedEmail,
        allowedEmailDomains = allowedEmailDomains?.split(',')?.filter { it.isNotBlank() } ?: emptyList(),
        defaultOrgId = defaultOrgId,
        defaultSupervisorId = defaultSupervisorId,
        accountTypeDictCode = accountTypeDictCode,
        accountStatusDictCode = accountStatusDictCode,
        defaultLocale = defaultLocale,
        defaultTimezone = defaultTimezone,
        defaultCurrency = defaultCurrency,
        configured = true,
    )

    private fun fail(errorCode: String, cause: Throwable? = null): Nothing =
        throw IdentityProviderJitConfigException(errorCode, cause)

    private companion object {
        const val MAX_ORG_ID_LENGTH = 36
        const val MAX_DOMAIN_LIST_LENGTH = 2048
        val LOCALE_PATTERN = Regex("^([A-Za-z]{2})(?:[-_]([A-Za-z]{2}))?$")
    }
}
