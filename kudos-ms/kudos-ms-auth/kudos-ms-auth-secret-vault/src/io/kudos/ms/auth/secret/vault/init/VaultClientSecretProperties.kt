package io.kudos.ms.auth.secret.vault.init

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.external-login.vault")
open class VaultClientSecretProperties {
    /** Allows the module to be installed but disabled when another resolver owns vault:. */
    var enabled: Boolean = true
    /** Server-owned Vault KV mount. References cannot override it. */
    var mount: String = "secret"
    /** HashiCorp KV backend API version. */
    var backendVersion: Int = 2
    /** Relative path namespaces which Provider references may address. Empty means deny all. */
    var allowedPathPrefixes: List<String> = listOf("oauth")
    /** Map key used when the reference omits #field. */
    var defaultKey: String = "client-secret"
}
