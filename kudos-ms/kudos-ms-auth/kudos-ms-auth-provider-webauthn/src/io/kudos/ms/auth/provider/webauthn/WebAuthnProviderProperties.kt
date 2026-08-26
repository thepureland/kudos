package io.kudos.ms.auth.provider.webauthn

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kudos.ms.auth.webauthn")
open class WebAuthnProviderProperties {
    var enabled: Boolean = false
    var rpId: String = ""
    var rpName: String = "Kudos"
    var origins: Set<String> = emptySet()
    var ceremonyTtlSeconds: Long = 300
    var browserTimeoutMillis: Long = 60_000
    var userVerification: String = "REQUIRED"
    var residentKey: String = "PREFERRED"
    var allowInsecureLocalhost: Boolean = false
    var operationReauthenticationMaxAgeSeconds: Long = 300
    var fidoMds: FidoMdsProperties = FidoMdsProperties()
}

open class FidoMdsProperties {
    var enabled: Boolean = false
    var cacheDirectory: String = ""
    var legalHeaders: Set<String> = emptySet()
    var refreshIntervalSeconds: Long = 3_600
}
