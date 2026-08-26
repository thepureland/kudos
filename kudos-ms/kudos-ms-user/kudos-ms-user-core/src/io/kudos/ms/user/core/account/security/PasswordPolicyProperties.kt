package io.kudos.ms.user.core.account.security

import org.springframework.boot.context.properties.ConfigurationProperties

/** Configurable defaults for local login and security passwords. */
@ConfigurationProperties(prefix = "kudos.ms.user.password-policy")
open class PasswordPolicyProperties {
    var enabled: Boolean = true
    var minLength: Int = 12
    var maxLength: Int = 64
    var rejectUsername: Boolean = true
    var rejectRepeatedCharacter: Boolean = true
    var requireUppercase: Boolean = false
    var requireLowercase: Boolean = false
    var requireDigit: Boolean = false
    var requireSpecial: Boolean = false
}
