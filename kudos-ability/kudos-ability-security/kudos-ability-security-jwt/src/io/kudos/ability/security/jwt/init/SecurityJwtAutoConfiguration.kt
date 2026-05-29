package io.kudos.ability.security.jwt.init

import com.nimbusds.jose.JWSAlgorithm
import com.nimbusds.jose.jwk.JWKSet
import com.nimbusds.jose.jwk.RSAKey
import com.nimbusds.jose.jwk.source.JWKSource
import com.nimbusds.jose.proc.JWSVerificationKeySelector
import com.nimbusds.jose.proc.SecurityContext
import com.nimbusds.jwt.proc.DefaultJWTProcessor
import io.kudos.ability.security.jwt.init.properties.SecurityJwtClaimProperties
import io.kudos.ability.security.jwt.init.properties.SecurityKeyProperties
import io.kudos.ability.security.jwt.support.JwtParametersTool
import io.kudos.base.logger.LogFactory
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.core.io.ResourceLoader
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder
import java.security.KeyPair
import java.security.KeyStore
import java.security.interfaces.RSAPrivateKey
import java.security.interfaces.RSAPublicKey

/**
 * Wires the JWT signing / verification chain off a PKCS12 keystore.
 *
 * Bean graph (all gated on `kudos.ability.security.jwt.key.key-store` being present):
 *  - [JWKSource] holds a single `RSAKey` derived from the configured keystore entry.
 *  - [JwtEncoder] is Nimbus-backed, mints RS256 tokens.
 *  - [JwtDecoder] verifies against RSA / EC / HMAC families (broad on purpose so a key rotation
 *    or family swap doesn't require a re-deploy of the verifier side). The default Nimbus
 *    claim-set verifier is disabled because [NimbusJwtDecoder] does its own — leaving both
 *    enabled double-validates `exp` and produces confusing error chains on expiry.
 *  - [JwtParametersTool] is a small helper for callers that want yml-driven default claims.
 *
 * Why `ConditionalOnProperty(key-store)`: the module is a "dependency-included but not yet
 * configured" friendly default — apps can pull it into their bundle and selectively enable JWT
 * by setting the keystore properties without excluding the dependency in profiles that don't
 * need it.
 *
 * Ported from soul's `SecurityJwtConfiguration`, cleaned up:
 *  - Dropped soul's `initPrivateKey` / `initPublicKey` private methods (dead — never called).
 *  - Dropped the `cert` field from `SecurityKeyProperties` (also never read).
 *  - Constructor-injected dependencies instead of field-injected `@Autowired` props.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(JwtEncoder::class)
@EnableConfigurationProperties(SecurityKeyProperties::class, SecurityJwtClaimProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-security-jwt.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class SecurityJwtAutoConfiguration : IComponentInitializer {

    private val log = LogFactory.getLog(this::class)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "kudos.ability.security.jwt.key", name = ["key-store"])
    open fun jwkSource(
        keyProperties: SecurityKeyProperties,
        resourceLoader: ResourceLoader,
    ): JWKSource<SecurityContext> {
        val keyPair = loadKeyPair(keyProperties, resourceLoader)
        val rsaKey = RSAKey.Builder(keyPair.public as RSAPublicKey)
            .privateKey(keyPair.private as RSAPrivateKey)
            .keyID("kid")
            .build()
        val jwkSet = JWKSet(rsaKey)
        return JWKSource { jwkSelector, _ -> jwkSelector.select(jwkSet) }
    }

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "kudos.ability.security.jwt.key", name = ["key-store"])
    open fun jwtEncoder(jwkSource: JWKSource<SecurityContext>): JwtEncoder =
        NimbusJwtEncoder(jwkSource)

    @Bean
    @ConditionalOnMissingBean
    @ConditionalOnProperty(prefix = "kudos.ability.security.jwt.key", name = ["key-store"])
    open fun jwtDecoder(jwkSource: JWKSource<SecurityContext>): JwtDecoder {
        val algorithms = mutableSetOf<JWSAlgorithm>().apply {
            addAll(JWSAlgorithm.Family.RSA)
            addAll(JWSAlgorithm.Family.EC)
            addAll(JWSAlgorithm.Family.HMAC_SHA)
        }
        val jwtProcessor = DefaultJWTProcessor<SecurityContext>().apply {
            jwsKeySelector = JWSVerificationKeySelector(algorithms, jwkSource)
            // NimbusJwtDecoder runs its own claim-set verification; suppress the default Nimbus
            // one so `exp` / `nbf` aren't checked twice and the error paths stay coherent.
            setJWTClaimsSetVerifier { _, _ -> }
        }
        return NimbusJwtDecoder(jwtProcessor)
    }

    @Bean
    @ConditionalOnMissingBean
    open fun jwtParametersTool(claimProperties: SecurityJwtClaimProperties): JwtParametersTool =
        JwtParametersTool(claimProperties)

    private fun loadKeyPair(props: SecurityKeyProperties, resourceLoader: ResourceLoader): KeyPair {
        val keyStorePath = requireNotNull(props.keyStore) {
            "kudos.ability.security.jwt.key.key-store must be set when JWT auto-config is active"
        }
        val resource = resourceLoader.getResource(keyStorePath)
        val pin = props.storePass.toCharArray()
        return try {
            val keyStore = KeyStore.getInstance("PKCS12").apply {
                resource.inputStream.use { load(it, pin) }
            }
            val privateKey = keyStore.getKey(props.alias, pin) as java.security.PrivateKey
            val certificate = keyStore.getCertificate(props.alias)
            KeyPair(certificate.publicKey, privateKey)
        } catch (e: Exception) {
            log.error(e, "Failed to load JWT keystore from {0}", keyStorePath)
            throw IllegalStateException("Failed to load JWT keystore: ${e.message}", e)
        }
    }

    override fun getComponentName() = "kudos-ability-security-jwt"
}
