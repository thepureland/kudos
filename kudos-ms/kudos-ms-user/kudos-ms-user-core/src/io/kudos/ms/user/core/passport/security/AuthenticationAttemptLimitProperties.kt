package io.kudos.ms.user.core.passport.security

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.user.passport.attempt-limit")
open class AuthenticationAttemptLimitProperties {
    var enabled: Boolean = true
    /** Redis/store errors deny authentication by default; explicitly opt in only for availability-first systems. */
    var failOpen: Boolean = false
    var ipMaxAttempts: Int = 120
    var ipWindowSeconds: Long = 60
    var principalMaxAttempts: Int = 20
    var principalWindowSeconds: Long = 60
    var passwordFailureMaxAttempts: Int = 5
    var passwordFailureWindowSeconds: Long = 900
    var totpFailureMaxAttempts: Int = 5
    var totpFailureWindowSeconds: Long = 300
    var recoveryCodeFailureMaxAttempts: Int = 5
    var recoveryCodeFailureWindowSeconds: Long = 900
    var emailOtpFailureMaxAttempts: Int = 5
    var emailOtpFailureWindowSeconds: Long = 900
}
