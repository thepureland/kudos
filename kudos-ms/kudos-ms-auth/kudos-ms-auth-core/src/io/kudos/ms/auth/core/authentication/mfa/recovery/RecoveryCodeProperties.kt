package io.kudos.ms.auth.core.authentication.mfa.recovery

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.mfa.recovery-code")
open class RecoveryCodeProperties {
    var codeCount: Int = 10
    var operationReauthenticationMaxAgeSeconds: Long = 300
}
