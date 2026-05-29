package io.kudos.ability.security.jwt.init

import io.kudos.ability.security.jwt.support.JwtParametersTool
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.springframework.boot.test.context.runner.ApplicationContextRunner
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtEncoder
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import java.time.Duration
import java.util.Date
import javax.security.auth.x500.X500Principal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Conditional-wiring + end-to-end integration tests for [SecurityJwtAutoConfiguration].
 *
 * Uses [ApplicationContextRunner] so each scenario spins up a minimal context with only the
 * auto-config under test. The keystore for the "active wiring" scenarios is generated
 * programmatically at @BeforeAll via Bouncy Castle, then written to a temp PKCS12 file pointed
 * to by the registered `kudos.ability.security.jwt.key.key-store` property.
 *
 * Coverage:
 *  - When the keystore property is absent, none of the JWT beans get created — depending on the
 *    module without configuring keys does not fail context refresh.
 *  - When the keystore property is set + the file is valid, encoder / decoder / parameters tool
 *    all wire successfully and round-trip an actual signed JWT (so the entire NimbusJwtEncoder ↔
 *    NimbusJwtDecoder + JWKSource chain is exercised end-to-end).
 *  - A custom-provided [JwtEncoder] bean wins over the auto-config's default
 *    (`@ConditionalOnMissingBean` honored).
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SecurityJwtAutoConfigurationTest {

    private val runner: ApplicationContextRunner = ApplicationContextRunner()
        .withUserConfiguration(SecurityJwtAutoConfiguration::class.java)

    @Test
    fun noKeystoreConfigured_jwtBeansAreNotRegistered() {
        runner.run { ctx ->
            assertEquals(
                0, ctx.getBeanNamesForType(JwtEncoder::class.java).size,
                "JwtEncoder must NOT wire when kudos.ability.security.jwt.key.key-store is absent",
            )
            assertEquals(
                0, ctx.getBeanNamesForType(JwtDecoder::class.java).size,
                "JwtDecoder must NOT wire either",
            )
            // JwtParametersTool has no key-store dependency, so it WILL be wired regardless — this
            // is intentional, lets apps use the claim-defaults helper to build JwtEncoderParameters
            // even before the keystore is provisioned.
            assertNotNull(ctx.getBean(JwtParametersTool::class.java))
        }
    }

    @Test
    fun keystoreConfigured_wiresFullEncoderDecoderChain_andRoundTripsAToken() {
        val keystoreFile = writeKeystore()
        runner
            .withPropertyValues(
                "kudos.ability.security.jwt.key.key-store=file:${keystoreFile.absolutePath}",
                "kudos.ability.security.jwt.key.store-pass=$KEYSTORE_PASSWORD",
                "kudos.ability.security.jwt.key.alias=$KEYSTORE_ALIAS",
                // iss must be a URL-shape string — Spring Security's JwtClaimAccessor.getIssuer()
                // converts to URL and fails on arbitrary strings.
                "kudos.ability.security.jwt.claims.iss=https://kudos-integration-test",
                "kudos.ability.security.jwt.claims.exp=3600",
                "kudos.ability.security.jwt.claims.jti=uuid()",
            )
            .run { ctx ->
                val encoder = ctx.getBean(JwtEncoder::class.java)
                val decoder = ctx.getBean(JwtDecoder::class.java)
                val params = ctx.getBean(JwtParametersTool::class.java)

                val token = encoder.encode(params.createDefault("alice"))
                val tokenValue = token.tokenValue
                assertEquals(3, tokenValue.split(".").size, "JWT compact form must be header.payload.signature")

                // End-to-end: same wiring decodes the same token and surfaces the claims we put in.
                val decoded = decoder.decode(tokenValue)
                assertEquals("alice", decoded.subject)
                assertEquals("https://kudos-integration-test", decoded.issuer.toString())
                assertNotNull(decoded.id, "uuid() jti must surface back through decoder")
                assertNotNull(decoded.expiresAt)
            }
    }

    @Test
    fun userProvidedJwtEncoder_takesPrecedence_overAutoConfig() {
        val keystoreFile = writeKeystore()
        val userEncoder = JwtEncoder { _ -> throw UnsupportedOperationException("custom encoder did not run") }
        runner
            .withPropertyValues(
                "kudos.ability.security.jwt.key.key-store=file:${keystoreFile.absolutePath}",
                "kudos.ability.security.jwt.key.store-pass=$KEYSTORE_PASSWORD",
                "kudos.ability.security.jwt.key.alias=$KEYSTORE_ALIAS",
            )
            .withBean(JwtEncoder::class.java, { userEncoder })
            .run { ctx ->
                val beans = ctx.getBeansOfType(JwtEncoder::class.java)
                assertEquals(1, beans.size, "ConditionalOnMissingBean must keep the autoconfig encoder off the bus")
                assertTrue(beans.values.single() === userEncoder)
            }
    }

    private fun writeKeystore(): File {
        if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
            Security.addProvider(BouncyCastleProvider())
        }
        val tempDir = Files.createTempDirectory("kudos-jwt-test").toFile().apply { deleteOnExit() }
        val keystorePath = File(tempDir, "test-jwt.p12").apply { deleteOnExit() }

        val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
        val notBefore = Date()
        val notAfter = Date(notBefore.time + Duration.ofDays(7).toMillis())
        val subject = X500Principal("CN=kudos-jwt-test")
        val builder = JcaX509v3CertificateBuilder(
            subject,
            BigInteger.valueOf(System.currentTimeMillis()),
            notBefore,
            notAfter,
            subject,
            keyPair.public,
        )
        val signer = JcaContentSignerBuilder("SHA256WithRSA")
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .build(keyPair.private)
        val cert: X509Certificate = JcaX509CertificateConverter()
            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
            .getCertificate(builder.build(signer))

        val keyStore = KeyStore.getInstance("PKCS12").apply { load(null, null) }
        keyStore.setKeyEntry(
            KEYSTORE_ALIAS,
            keyPair.private,
            KEYSTORE_PASSWORD.toCharArray(),
            arrayOf(cert),
        )
        keystorePath.outputStream().use { keyStore.store(it, KEYSTORE_PASSWORD.toCharArray()) }
        return keystorePath
    }

    companion object {
        private const val KEYSTORE_ALIAS = "kudos-jwt-test-key"
        private const val KEYSTORE_PASSWORD = "kudos-test-pwd"
    }
}
