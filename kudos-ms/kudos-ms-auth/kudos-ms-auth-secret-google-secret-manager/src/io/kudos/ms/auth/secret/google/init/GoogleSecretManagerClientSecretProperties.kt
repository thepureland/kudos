package io.kudos.ms.auth.secret.google.init

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.external-login.google-secret-manager")
open class GoogleSecretManagerClientSecretProperties {
    /** Allows another implementation to own gcp-sm:. */
    var enabled: Boolean = true
    /** Project ids or numbers which references may select. Empty means deny all. */
    var allowedProjectIds: List<String> = emptyList()
    /** Flat secret-id prefixes allowed inside an approved project. Empty means deny all. */
    var allowedSecretIdPrefixes: List<String> = listOf("kudos-auth-")
    /** Server-owned version: latest for normal rotation, or an explicit positive version number. */
    var version: String = "latest"
}
