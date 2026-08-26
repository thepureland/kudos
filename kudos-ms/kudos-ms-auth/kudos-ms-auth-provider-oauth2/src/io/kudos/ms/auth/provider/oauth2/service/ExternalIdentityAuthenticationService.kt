package io.kudos.ms.auth.provider.oauth2.service

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.common.provider.enums.ExternalProtocolEnum
import io.kudos.ms.auth.common.provider.enums.ExternalJitPolicyEnum
import io.kudos.ms.auth.common.provider.vo.ExternalPrincipal
import io.kudos.ms.auth.core.provider.dao.AuthIdentityProviderDao
import io.kudos.ms.auth.core.provider.dao.AuthProviderTemplateDao
import io.kudos.ms.auth.core.provider.claim.service.iservice.IIdentityProviderClaimMappingService
import io.kudos.ms.auth.core.provider.invitation.model.ExternalIdentityInvitationException
import io.kudos.ms.auth.core.provider.invitation.service.iservice.IExternalIdentityInvitationService
import io.kudos.ms.auth.core.provider.jit.model.EffectiveIdentityProviderJitConfig
import io.kudos.ms.auth.core.provider.jit.model.IdentityProviderJitConfigException
import io.kudos.ms.auth.core.provider.jit.service.iservice.IIdentityProviderJitConfigService
import io.kudos.ms.user.core.account.model.ExternalAccountBindingCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningException
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import io.kudos.ms.user.core.login.model.UserLoginAttempt
import io.kudos.ms.user.core.login.service.iservice.IUserLogLoginService
import org.springframework.security.oauth2.core.oidc.user.OidcUser
import org.springframework.security.oauth2.core.user.OAuth2User
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.security.MessageDigest
import java.net.IDN
import java.time.LocalDateTime
import java.util.Locale

/** Maps verified Spring principals to Kudos identities and accepts bound accounts only. */
@Service
open class ExternalIdentityAuthenticationService(
    private val identityProviderDao: AuthIdentityProviderDao,
    private val providerTemplateDao: AuthProviderTemplateDao,
    private val claimMappingService: IIdentityProviderClaimMappingService,
    private val invitationService: IExternalIdentityInvitationService,
    private val userAccountThirdService: IUserAccountThirdService,
    private val jitConfigService: IIdentityProviderJitConfigService,
    private val externalAccountProvisioningService: IExternalAccountProvisioningService,
    private val userAccountService: IUserAccountService,
    private val userLogLoginService: IUserLogLoginService,
) {

    private val log = LogFactory.getLog(this::class)

    @Transactional
    open fun authenticate(
        providerId: String,
        oauth2User: OAuth2User,
        loginIp: Long?,
        userAgent: String?,
        invitationId: String? = null,
    ): BoundExternalIdentity {
        val resolved = resolve(providerId, oauth2User)
        val externalPrincipal = resolved.principal
        val issuer = externalPrincipal.issuer
        val subject = externalPrincipal.subject
        val providerCode = resolved.providerCode
        var binding = userAccountThirdService.getByIdentityProviderSubject(
            resolved.tenantId,
            resolved.providerId,
            issuer,
            subject,
        ) ?: userAccountThirdService.getByProviderSubject(
            resolved.tenantId,
            providerCode,
            issuer,
            subject,
        )?.takeIf { it.identityProviderId == null }

        if (binding == null || binding.active != true) {
            val provider = identityProviderDao.findActiveById(resolved.providerId)
                ?: throw ExternalIdentityAuthenticationException("EXTERNAL_PROVIDER_NOT_AVAILABLE")
            val jitPolicy = runCatching { ExternalJitPolicyEnum.valueOf(provider.jitPolicy.uppercase()) }.getOrNull()
            binding = when (jitPolicy) {
                ExternalJitPolicyEnum.INVITE_ONLY -> {
                    val requiredInvitationId = invitationId?.takeIf { it.isNotBlank() }
                        ?: denyUnbound(resolved.tenantId, subject, loginIp, userAgent, "EXTERNAL_INVITATION_REQUIRED")
                    val invitedUserId = try {
                        invitationService.consume(
                            requiredInvitationId,
                            resolved.tenantId,
                            resolved.providerId,
                            externalPrincipal,
                        )
                    } catch (e: ExternalIdentityInvitationException) {
                        denyUnbound(resolved.tenantId, subject, loginIp, userAgent, e.errorCode, e)
                    }
                    userAccountThirdService.bindExternalIdentity(
                        ExternalAccountBindingCommand(
                            userId = invitedUserId,
                            tenantId = resolved.tenantId,
                            identityProviderId = resolved.providerId,
                            providerCode = providerCode,
                            issuer = issuer,
                            subject = subject,
                            unionId = externalPrincipal.unionId,
                            displayName = externalPrincipal.displayName,
                            email = externalPrincipal.email,
                            avatarUrl = externalPrincipal.avatarUrl,
                        )
                    )
                }

                ExternalJitPolicyEnum.JIT_CREATE -> provisionJitAccount(resolved, loginIp, userAgent)

                else -> denyUnbound(
                    resolved.tenantId,
                    subject,
                    loginIp,
                    userAgent,
                    "EXTERNAL_IDENTITY_NOT_BOUND",
                )
            }
        }
        val activeBinding = requireNotNull(binding)
        val user = userAccountService.get(activeBinding.userId)
        val now = LocalDateTime.now()
        val frozen = user?.freezeType != null &&
            (user.freezeStartTime == null || !now.isBefore(user.freezeStartTime)) &&
            (user.freezeEndTime == null || now.isBefore(user.freezeEndTime))
        if (user == null || user.tenantId != resolved.tenantId || user.active != true || frozen) {
            recordAttempt(resolved.tenantId, activeBinding.userId, subject, loginIp, userAgent, false, "ACCOUNT_UNAVAILABLE")
            throw ExternalIdentityAuthenticationException("ACCOUNT_UNAVAILABLE")
        }

        activeBinding.lastLoginTime = now
        runCatching { userAccountThirdService.updateLastLoginTime(activeBinding.id, activeBinding.lastLoginTime!!) }
            .onFailure { log.warn("Could not update external identity last-login time: ${it.message}") }
        loginIp?.let { userAccountService.updateLastLoginInfo(user.id, it, now) }
        recordAttempt(resolved.tenantId, user.id, user.username, loginIp, userAgent, true, null)
        return BoundExternalIdentity(user.id, resolved.tenantId, user.username, providerCode, externalPrincipal)
    }

    private fun provisionJitAccount(
        resolved: ResolvedExternalPrincipal,
        loginIp: Long?,
        userAgent: String?,
    ) = try {
        val principal = resolved.principal
        val config = try {
            jitConfigService.getEffective(resolved.providerId, resolved.tenantId)
        } catch (e: IdentityProviderJitConfigException) {
            denyUnbound(resolved.tenantId, principal.subject, loginIp, userAgent, e.errorCode, e)
        }
        validateJitEmail(config, principal.email, principal.emailVerified, resolved, loginIp, userAgent)
        externalAccountProvisioningService.provision(
            ExternalAccountProvisioningCommand(
                username = ExternalJitUsernameGenerator.generate(resolved, config.usernameStrategy),
                tenantId = resolved.tenantId,
                identityProviderId = resolved.providerId,
                providerCode = resolved.providerCode,
                issuer = principal.issuer,
                subject = principal.subject,
                unionId = principal.unionId,
                displayName = principal.displayName,
                email = principal.email,
                avatarUrl = principal.avatarUrl,
                defaultLocale = config.defaultLocale ?: normalizeLocale(principal.locale),
                defaultTimezone = config.defaultTimezone,
                defaultCurrency = config.defaultCurrency,
                defaultOrgId = config.defaultOrgId,
                defaultSupervisorId = config.defaultSupervisorId,
                accountTypeDictCode = config.accountTypeDictCode,
                accountStatusDictCode = config.accountStatusDictCode,
            )
        )
    } catch (e: ExternalAccountProvisioningException) {
        // A concurrent callback may have committed the same stable identity while this transaction rolled back.
        userAccountThirdService.getByIdentityProviderSubject(
            resolved.tenantId,
            resolved.providerId,
            resolved.principal.issuer,
            resolved.principal.subject,
        )?.takeIf { it.active == true } ?: denyUnbound(
            resolved.tenantId,
            resolved.principal.subject,
            loginIp,
            userAgent,
            e.errorCode,
            e,
        )
    }

    private fun validateJitEmail(
        config: EffectiveIdentityProviderJitConfig,
        rawEmail: String?,
        emailVerified: Boolean?,
        resolved: ResolvedExternalPrincipal,
        loginIp: Long?,
        userAgent: String?,
    ) {
        if (!config.requireVerifiedEmail && config.allowedEmailDomains.isEmpty()) return
        val email = rawEmail?.trim()?.takeIf { it.isNotBlank() }
            ?: denyUnbound(resolved.tenantId, resolved.principal.subject, loginIp, userAgent, "EXTERNAL_JIT_EMAIL_REQUIRED")
        if (emailVerified != true) {
            denyUnbound(resolved.tenantId, resolved.principal.subject, loginIp, userAgent, "EXTERNAL_JIT_EMAIL_NOT_VERIFIED")
        }
        if (config.allowedEmailDomains.isEmpty()) return
        if (email.count { it == '@' } != 1) {
            denyUnbound(resolved.tenantId, resolved.principal.subject, loginIp, userAgent, "EXTERNAL_JIT_EMAIL_INVALID")
        }
        val rawDomain = email.substringAfterLast('@', missingDelimiterValue = "").trim().lowercase(Locale.ROOT)
        val domain = runCatching { IDN.toASCII(rawDomain, IDN.USE_STD3_ASCII_RULES).lowercase(Locale.ROOT) }
            .getOrNull()
            ?: denyUnbound(resolved.tenantId, resolved.principal.subject, loginIp, userAgent, "EXTERNAL_JIT_EMAIL_INVALID")
        val allowed = config.allowedEmailDomains.any { rule ->
            if (rule.startsWith("*.")) {
                domain.endsWith(".${rule.removePrefix("*.")}")
            } else {
                domain == rule
            }
        }
        if (!allowed) {
            denyUnbound(resolved.tenantId, resolved.principal.subject, loginIp, userAgent, "EXTERNAL_JIT_EMAIL_DOMAIN_NOT_ALLOWED")
        }
    }

    private fun normalizeLocale(value: String?): String? {
        val locale = value?.trim()?.takeIf { it.isNotBlank() }
            ?.replace('_', '-')
            ?.let(Locale::forLanguageTag)
            ?: return null
        val language = locale.language.lowercase(Locale.ROOT).takeIf { it.length == 2 } ?: return null
        val country = locale.country.uppercase(Locale.ROOT).takeIf { it.length == 2 }
        return if (country == null) language else "${language}_${country}"
    }

    private fun denyUnbound(
        tenantId: String,
        subject: String,
        loginIp: Long?,
        userAgent: String?,
        errorCode: String,
        cause: Throwable? = null,
    ): Nothing {
        recordAttempt(tenantId, null, subject, loginIp, userAgent, false, errorCode)
        throw ExternalIdentityAuthenticationException(errorCode, cause)
    }

    /** Resolves only data already protocol-validated by Spring; it performs no account lookup. */
    open fun resolve(providerId: String, oauth2User: OAuth2User): ResolvedExternalPrincipal {
        val provider = identityProviderDao.findActiveById(providerId)
            ?: throw ExternalIdentityAuthenticationException("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        val template = providerTemplateDao.get(provider.templateId)?.takeIf { it.active }
            ?: throw ExternalIdentityAuthenticationException("EXTERNAL_PROVIDER_NOT_AVAILABLE")
        val protocol = runCatching { ExternalProtocolEnum.valueOf(template.protocol.uppercase()) }
            .getOrElse { throw ExternalIdentityAuthenticationException("EXTERNAL_PROTOCOL_NOT_SUPPORTED") }
        val attributes = oauth2User.attributes.toMap()
        val claimMapping = runCatching { claimMappingService.getEffective(provider.id, provider.tenantId) }
            .getOrElse { throw ExternalIdentityAuthenticationException("EXTERNAL_CLAIM_MAPPING_INVALID", it) }
        val issuer = if (oauth2User is OidcUser) {
            oauth2User.issuer?.toString()
        } else {
            provider.issuer ?: template.issuer
        }
        val subject = if (oauth2User is OidcUser) {
            oauth2User.subject
        } else {
            ExternalClaimResolver.firstString(attributes, claimMapping.subjectClaims)
        }?.takeIf { it.isNotBlank() }
            ?: throw ExternalIdentityAuthenticationException("EXTERNAL_SUBJECT_MISSING")
        val externalPrincipal = ExternalPrincipal(
            providerId = provider.id,
            protocol = protocol,
            issuer = issuer,
            subject = subject,
            unionId = ExternalClaimResolver.firstString(attributes, claimMapping.unionIdClaims),
            username = ExternalClaimResolver.firstString(attributes, claimMapping.usernameClaims),
            displayName = ExternalClaimResolver.firstString(attributes, claimMapping.displayNameClaims),
            email = ExternalClaimResolver.firstString(attributes, claimMapping.emailClaims),
            emailVerified = ExternalClaimResolver.first(attributes, claimMapping.emailVerifiedClaims).asBoolean(),
            phone = ExternalClaimResolver.firstString(attributes, claimMapping.phoneClaims),
            phoneVerified = ExternalClaimResolver.first(attributes, claimMapping.phoneVerifiedClaims).asBoolean(),
            avatarUrl = ExternalClaimResolver.firstString(attributes, claimMapping.avatarClaims),
            locale = ExternalClaimResolver.firstString(attributes, claimMapping.localeClaims),
            rawClaims = attributes,
        )
        return ResolvedExternalPrincipal(
            tenantId = provider.tenantId,
            providerId = provider.id,
            providerCode = template.code.lowercase(),
            principal = externalPrincipal,
        )
    }

    private fun recordAttempt(
        tenantId: String,
        userId: String?,
        identifier: String,
        loginIp: Long?,
        userAgent: String?,
        success: Boolean,
        failureReason: String?,
    ) {
        val username = if (userId == null) sha256(identifier).take(32) else identifier
        runCatching {
            userLogLoginService.recordLoginAttempt(
                UserLoginAttempt(
                    userId = userId,
                    username = username,
                    tenantId = tenantId,
                    loginTime = LocalDateTime.now(),
                    loginIp = loginIp,
                    userAgent = userAgent,
                    loginSuccess = success,
                    failureReason = failureReason,
                )
            )
        }.onFailure { log.error(it, "Failed to persist external login audit") }
    }

    private fun Any?.asBoolean(): Boolean? = when (this) {
        is Boolean -> this
        is String -> toBooleanStrictOrNull()
        else -> null
    }

    private fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray(Charsets.UTF_8))
        .joinToString("") { "%02x".format(it) }
}
