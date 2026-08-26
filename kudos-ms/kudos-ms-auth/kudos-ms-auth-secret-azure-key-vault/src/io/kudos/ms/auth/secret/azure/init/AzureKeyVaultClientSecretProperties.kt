package io.kudos.ms.auth.secret.azure.init

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.external-login.azure-key-vault")
open class AzureKeyVaultClientSecretProperties {
    /** Allows another implementation to own azure-kv:. */
    var enabled: Boolean = true
    /** Reference alias to exact SecretClient vault URL. Empty means deny every vault. */
    var vaultUrls: Map<String, String> = emptyMap()
    /** Flat secret-name prefixes allowed inside an approved vault. Empty means deny all. */
    var allowedSecretNamePrefixes: List<String> = listOf("kudos-auth-")
    /** Server-owned version. Empty reads the latest enabled version; otherwise use a 32-character version id. */
    var version: String = ""
}
