package io.kudos.ms.auth.core.authentication.mfa

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.mfa")
open class TotpEnrollmentProperties {
    var issuer: String = "Kudos"
    var enrollmentTtlSeconds: Long = 300
    var enrollmentMaxFailedAttempts: Int = 5
    var operationReauthenticationMaxAgeSeconds: Long = 300
}
