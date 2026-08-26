package io.kudos.ms.auth.provider.oauth2.init

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.external-login")
open class ExternalLoginProperties {
    var successPath: String = "/"
    var failurePath: String = "/"
    var linkSuccessPath: String = "/"
    var linkFailurePath: String = "/"
    var linkReauthenticationMaxAgeSeconds: Long = 300
    /** TTL for the server-side PKCE verifier, OIDC nonce and authorization request snapshot. */
    var authorizationRequestTtlSeconds: Long = 300
    /** Disabled by default so source-side rotation is visible immediately; successful values are never cached remotely. */
    var secretCacheTtlSeconds: Long = 0
    /** Built-in env resolver is denied outside these prefixes. Empty means deny all env references. */
    var allowedSecretEnvironmentPrefixes: List<String> = listOf("KUDOS_AUTH_")
    /** Built-in property resolver is denied outside these prefixes. Empty means deny all property references. */
    var allowedSecretPropertyPrefixes: List<String> = listOf("kudos.ms.auth.external-secrets.")

    fun requireSafeLocalPath(value: String): String {
        require(value.startsWith('/') && !value.startsWith("//")) { "External login redirect must be a local path" }
        require('\r' !in value && '\n' !in value) { "External login redirect contains control characters" }
        return value
    }
}
