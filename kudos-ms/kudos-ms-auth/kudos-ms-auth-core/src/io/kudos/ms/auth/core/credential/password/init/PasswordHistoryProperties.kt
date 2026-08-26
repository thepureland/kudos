package io.kudos.ms.auth.core.credential.password.init

import org.springframework.boot.context.properties.ConfigurationProperties

/** Retention settings for password hashes that must not be reused. */
@ConfigurationProperties(prefix = "kudos.ms.auth.credential.password-history")
open class PasswordHistoryProperties {
    var enabled: Boolean = true
    var historySize: Int = 5
}
