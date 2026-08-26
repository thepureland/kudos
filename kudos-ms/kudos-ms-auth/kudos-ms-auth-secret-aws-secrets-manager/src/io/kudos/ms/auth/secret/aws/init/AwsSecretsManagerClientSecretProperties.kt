package io.kudos.ms.auth.secret.aws.init

import org.springframework.boot.context.properties.ConfigurationProperties

@ConfigurationProperties(prefix = "kudos.ms.auth.external-login.aws-secrets-manager")
open class AwsSecretsManagerClientSecretProperties {
    /** Allows another implementation to own aws-sm:. */
    var enabled: Boolean = true
    /** Secret-name namespaces allowed for non-ARN references. Empty means deny all names. */
    var allowedSecretIdPrefixes: List<String> = listOf("kudos/auth")
    /** ARN references are disabled unless both this flag and an ARN prefix policy are configured. */
    var allowArns: Boolean = false
    /** Literal ARN prefixes normally pin partition, region, account and secret-name namespace. */
    var allowedArnPrefixes: List<String> = emptyList()
    /** Server-owned rotation stage. References cannot select a version id or stage. */
    var versionStage: String = "AWSCURRENT"
}
