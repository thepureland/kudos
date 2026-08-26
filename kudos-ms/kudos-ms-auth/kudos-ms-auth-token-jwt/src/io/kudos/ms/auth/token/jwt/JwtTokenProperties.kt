package io.kudos.ms.auth.token.jwt

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties("kudos.ms.auth.token.jwt")
open class JwtTokenProperties {
    var enabled: Boolean = false
    var issuer: String = "kudos"
    var audience: String = "kudos-api"
    var accessTtlSeconds: Long = 300
    var signingAlgorithm: String = "RS256"
    /** Required when the encoder exposes multiple active signing keys during rotation. */
    var keyId: String? = null
}
