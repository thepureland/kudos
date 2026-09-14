package io.kudos.ms.auth.provider.emailotp

import org.springframework.boot.context.properties.ConfigurationProperties

/** Deployment-owned policy and secret material for passwordless email authentication. */
@ConfigurationProperties(prefix = "kudos.ms.auth.email-otp")
open class EmailOtpProperties {
    var enabled: Boolean = false
    var codeTtlSeconds: Long = 600
    var maxVerificationAttempts: Int = 5
    var codeDigits: Int = 6
    var autoProvision: Boolean = false
    var identityProviderId: String = "kudos-email-otp"
    var providerCode: String = "email_otp"
    var defaultLocale: String? = null
    var defaultTimezone: String? = null
    var defaultCurrency: String? = null
    var defaultOrgId: String? = null
    var defaultSupervisorId: String? = null
    var accountTypeDictCode: String? = null
    var accountStatusDictCode: String? = null

    /**
     * HMAC key used to make the low-entropy code resistant to an offline store dump.
     * There is deliberately no default; enabled deployments must supply at least 32 UTF-8 bytes.
     */
    var codeHmacSecret: String = ""

    fun validate() {
        require(codeTtlSeconds in 60..1_800) { "email OTP ttl must be between 60 and 1800 seconds" }
        require(maxVerificationAttempts in 1..10) { "email OTP attempts must be between 1 and 10" }
        require(codeDigits in 6..8) { "email OTP digits must be between 6 and 8" }
        require(!autoProvision || identityProviderId.isNotBlank()) {
            "email OTP identity-provider-id is required when auto-provision is enabled"
        }
        require(!autoProvision || providerCode.isNotBlank()) {
            "email OTP provider-code is required when auto-provision is enabled"
        }
        require(codeHmacSecret.toByteArray(Charsets.UTF_8).size >= 32) {
            "kudos.ms.auth.email-otp.code-hmac-secret must contain at least 32 UTF-8 bytes"
        }
    }
}
