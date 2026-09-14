package io.kudos.ms.auth.provider.emailotp.identity

import io.kudos.ms.auth.provider.emailotp.EmailOtpProperties
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningCommand
import io.kudos.ms.user.core.account.model.ExternalAccountProvisioningException
import io.kudos.ms.user.core.account.model.po.UserAccountThird
import io.kudos.ms.user.core.account.service.iservice.IExternalAccountProvisioningService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.service.iservice.IUserAccountThirdService
import java.security.MessageDigest
import java.time.LocalDateTime
import java.util.Locale

/** Maps a proved email address to Kudos user master data and optionally performs JIT registration. */
open class KudosEmailOtpPrincipalService(
    private val thirdService: IUserAccountThirdService,
    private val provisioningService: IExternalAccountProvisioningService,
    private val accountService: IUserAccountService,
    private val properties: EmailOtpProperties,
) : IEmailOtpPrincipalService {

    override fun resolveOrProvision(tenantId: String, email: String): EmailOtpPrincipal {
        require(tenantId.isNotBlank()) { "tenantId must not be blank" }
        val normalizedEmail = email.trim().lowercase(Locale.ROOT)
        require(normalizedEmail.isNotBlank()) { "email must not be blank" }

        val binding = findBinding(tenantId, normalizedEmail)
            ?: provision(tenantId, normalizedEmail)
        if (binding.active != true) throw EmailOtpPrincipalException("EMAIL_OTP_IDENTITY_DISABLED")

        val user = accountService.get(binding.userId)
            ?: throw EmailOtpPrincipalException("EMAIL_OTP_ACCOUNT_UNAVAILABLE")
        val now = LocalDateTime.now()
        val frozen = user.freezeType != null &&
            (user.freezeStartTime == null || !now.isBefore(user.freezeStartTime)) &&
            (user.freezeEndTime == null || now.isBefore(user.freezeEndTime))
        if (user.tenantId != tenantId || user.active != true || frozen || user.username.isNullOrBlank()) {
            throw EmailOtpPrincipalException("EMAIL_OTP_ACCOUNT_UNAVAILABLE")
        }
        return EmailOtpPrincipal(user.id, tenantId, requireNotNull(user.username))
    }

    private fun provision(tenantId: String, email: String): UserAccountThird {
        if (!properties.autoProvision) {
            throw EmailOtpPrincipalException("EMAIL_OTP_IDENTITY_NOT_BOUND")
        }
        return try {
            provisioningService.provision(
                ExternalAccountProvisioningCommand(
                    username = stableUsername(tenantId, email),
                    tenantId = tenantId,
                    identityProviderId = properties.identityProviderId,
                    providerCode = properties.providerCode,
                    issuer = ISSUER,
                    subject = email,
                    displayName = email.substringBefore('@').take(64),
                    email = email,
                    emailVerified = true,
                    defaultLocale = properties.defaultLocale,
                    defaultTimezone = properties.defaultTimezone,
                    defaultCurrency = properties.defaultCurrency,
                    defaultOrgId = properties.defaultOrgId,
                    defaultSupervisorId = properties.defaultSupervisorId,
                    accountTypeDictCode = properties.accountTypeDictCode,
                    accountStatusDictCode = properties.accountStatusDictCode,
                )
            )
        } catch (e: ExternalAccountProvisioningException) {
            findBinding(tenantId, email) ?: throw EmailOtpPrincipalException(e.errorCode, e)
        }
    }

    private fun findBinding(tenantId: String, email: String): UserAccountThird? =
        thirdService.getByIdentityProviderSubject(
            tenantId,
            properties.identityProviderId,
            ISSUER,
            email,
        )

    private fun stableUsername(tenantId: String, email: String): String {
        val input = "$tenantId\u0000$email".toByteArray(Charsets.UTF_8)
        val digest = MessageDigest.getInstance("SHA-256").digest(input)
            .joinToString("") { "%02x".format(it.toInt() and 0xff) }
        return "email_${digest.take(26)}"
    }

    private companion object {
        const val ISSUER = "urn:kudos:email-otp"
    }
}

class EmailOtpPrincipalException(
    val errorCode: String,
    cause: Throwable? = null,
) : RuntimeException(errorCode, cause)
