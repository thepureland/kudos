package io.kudos.ability.security.jwt.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Configuration for the PKCS12 keystore that backs JWT signing / verification.
 *
 * Bound from `kudos.ability.security.jwt.key.*` in `application.yml`. The keystore must contain
 * an RSA private + matching certificate under [alias]; the JWT auto-config loads it once at
 * context refresh, derives the [java.security.KeyPair], and wires it into the JWKSource that
 * Spring Security's [org.springframework.security.oauth2.jwt.NimbusJwtEncoder] /
 * [org.springframework.security.oauth2.jwt.NimbusJwtDecoder] use.
 *
 * Soul's port additionally exposed a `cert` field for a separate X.509 certificate path; the
 * code never read it (`initPublicKey` was unreferenced dead code). The kudos port drops it —
 * keystore-only is the supported path, and apps with a separate cert need to declare their own
 * [org.springframework.security.oauth2.jwt.JwtDecoder] bean.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.security.jwt.key")
class SecurityKeyProperties {

    /**
     * Spring resource location of the PKCS12 keystore. Common patterns:
     *  - `classpath:my-jwt.p12` — bundled with the jar (fine for dev; rotate via deployment).
     *  - `file:/etc/secrets/jwt.p12` — externalised (preferred for prod; volumes / secret mounts).
     *
     * When blank/missing, the auto-config's [ConditionalOnProperty] backs off and no JWT beans
     * are registered — apps can still depend on this module without forcing a keystore.
     */
    var keyStore: String? = null

    /** Password protecting the keystore file. */
    var storePass: String = ""

    /** Alias of the key entry inside the keystore. */
    var alias: String = ""
}
