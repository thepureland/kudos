package io.kudos.ability.security.jwt.resourceserver

import io.kudos.ability.security.jwt.resourceserver.JwtIssuerAudienceValidationIT.Companion.dynamicProperties
import io.kudos.test.common.init.EnableKudosTest
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder
import org.bouncycastle.jce.provider.BouncyCastleProvider
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Import
import org.springframework.http.MediaType
import org.springframework.security.oauth2.jose.jws.SignatureAlgorithm
import org.springframework.security.oauth2.jwt.JwsHeader
import org.springframework.security.oauth2.jwt.JwtClaimsSet
import org.springframework.security.oauth2.jwt.JwtEncoder
import org.springframework.security.oauth2.jwt.JwtEncoderParameters
import org.springframework.test.context.DynamicPropertyRegistry
import org.springframework.test.context.DynamicPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RestController
import java.io.File
import java.math.BigInteger
import java.nio.file.Files
import java.security.KeyPairGenerator
import java.security.KeyStore
import java.security.Security
import java.security.cert.X509Certificate
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID
import javax.security.auth.x500.X500Principal
import kotlin.test.Test

/**
 * Integration test for the issuer / audience validation knobs of the JWT resource-server
 * autoconfig — the fence against lateral token replay when several services share one signing
 * keystore.
 *
 * Same harness as [JwtResourceServerFilterChainIT] (BC-generated PKCS12 keystore + real
 * JwtEncoder + MockMvc through the full filter chain), but the context is booted with
 * `kudos.ability.security.jwt.resource-server.issuer` / `.audience` set, so the validator chain
 * includes `JwtValidators.createDefaultWithIssuer(...)` plus the kudos `JwtAudienceValidator`.
 *
 * The negative cases all sign with the SAME keystore — the point is precisely that a
 * cryptographically valid signature is not enough once iss / aud are pinned:
 *  - correct iss + aud → 200
 *  - wrong iss (another service's tokens) → 401
 *  - wrong aud → 401
 *  - missing iss / aud claims entirely → 401 (omitting the claims must not bypass the checks)
 *
 * The "both knobs blank → no validation" backward-compat path is covered in
 * [JwtResourceServerFilterChainIT], whose context runs without these properties.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@AutoConfigureMockMvc
@Import(JwtIssuerAudienceValidationIT.TestControllers::class)
internal class JwtIssuerAudienceValidationIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var encoder: JwtEncoder

    @Test
    fun tokenWithCorrectIssuerAndAudience_returns200() {
        val token = mintToken(issuer = EXPECTED_ISSUER, audience = listOf(EXPECTED_AUDIENCE))
        mockMvc.get("/secured") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            content { string("hello alice") }
        }
    }

    @Test
    fun tokenWithWrongIssuer_returns401() {
        // Signed with the same keystore but minted by "another service": signature verifies,
        // issuer pinning must still reject — this is the lateral-replay scenario.
        val token = mintToken(issuer = "https://some-other-service", audience = listOf(EXPECTED_AUDIENCE))
        mockMvc.get("/secured") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun tokenWithWrongAudience_returns401() {
        val token = mintToken(issuer = EXPECTED_ISSUER, audience = listOf("other-consumer"))
        mockMvc.get("/secured") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun tokenWithoutIssuerAndAudienceClaims_returns401() {
        // Omitting the claims must not bypass the pinned checks — otherwise any minter could
        // defeat validation by simply not writing iss / aud.
        val token = mintToken(issuer = null, audience = null)
        mockMvc.get("/secured") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun audienceListContainingExpectedValueAmongOthers_returns200() {
        // RFC 7519 aud is a list; a token addressed to several services, this one included,
        // must pass.
        val token = mintToken(
            issuer = EXPECTED_ISSUER,
            audience = listOf("other-consumer", EXPECTED_AUDIENCE),
        )
        mockMvc.get("/secured") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    private fun mintToken(issuer: String?, audience: List<String>?): String {
        val iat = Instant.now()
        val builder = JwtClaimsSet.builder()
            .subject("alice")
            .issuedAt(iat)
            .expiresAt(iat.plus(Duration.ofMinutes(5)))
            .id(UUID.randomUUID().toString())
        if (issuer != null) builder.issuer(issuer)
        if (audience != null) builder.audience(audience)
        return encoder.encode(
            JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), builder.build()),
        ).tokenValue
    }

    @TestConfiguration
    internal open class TestControllers {
        @Bean
        open fun securedController(): SecuredController = SecuredController()
    }

    @RestController
    internal class SecuredController {
        @GetMapping("/secured", produces = [MediaType.TEXT_PLAIN_VALUE])
        fun secured(@org.springframework.security.core.annotation.AuthenticationPrincipal jwt: org.springframework.security.oauth2.jwt.Jwt): String =
            "hello ${jwt.subject}"
    }

    companion object {
        private const val EXPECTED_ISSUER = "https://kudos-it-issuer"
        private const val EXPECTED_AUDIENCE = "kudos-it-audience"
        private const val KEYSTORE_ALIAS = "kudos-jwt-issaud-it-key"
        private const val KEYSTORE_PASSWORD = "kudos-it-pwd"

        // Same lazy keystore-generation pattern as JwtResourceServerFilterChainIT: built once per
        // test JVM, no binary blob in the repo.
        private val keystorePath: String by lazy { writeKeystore().absolutePath }

        private fun writeKeystore(): File {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
            val tempDir = Files.createTempDirectory("kudos-jwt-issaud-it").toFile().apply { deleteOnExit() }
            val keystoreFile = File(tempDir, "test-jwt.p12").apply { deleteOnExit() }

            val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val notBefore = Date()
            val notAfter = Date(notBefore.time + Duration.ofDays(7).toMillis())
            val subject = X500Principal("CN=kudos-jwt-issaud-it")
            val cert: X509Certificate = JcaX509CertificateConverter()
                .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                .getCertificate(
                    JcaX509v3CertificateBuilder(
                        subject,
                        BigInteger.valueOf(System.currentTimeMillis()),
                        notBefore,
                        notAfter,
                        subject,
                        keyPair.public,
                    ).build(
                        JcaContentSignerBuilder("SHA256WithRSA")
                            .setProvider(BouncyCastleProvider.PROVIDER_NAME)
                            .build(keyPair.private),
                    ),
                )

            val keyStore = KeyStore.getInstance("PKCS12").apply { load(null, null) }
            keyStore.setKeyEntry(
                KEYSTORE_ALIAS,
                keyPair.private,
                KEYSTORE_PASSWORD.toCharArray(),
                arrayOf(cert),
            )
            keystoreFile.outputStream().use { keyStore.store(it, KEYSTORE_PASSWORD.toCharArray()) }
            return keystoreFile
        }

        @JvmStatic
        @DynamicPropertySource
        fun dynamicProperties(registry: DynamicPropertyRegistry) {
            registry.add("kudos.ability.security.jwt.key.key-store") { "file:$keystorePath" }
            registry.add("kudos.ability.security.jwt.key.store-pass") { KEYSTORE_PASSWORD }
            registry.add("kudos.ability.security.jwt.key.alias") { KEYSTORE_ALIAS }
            registry.add("kudos.ability.security.jwt.resource-server.enabled") { "true" }
            registry.add("kudos.ability.security.jwt.resource-server.issuer") { EXPECTED_ISSUER }
            registry.add("kudos.ability.security.jwt.resource-server.audience") { EXPECTED_AUDIENCE }
        }
    }
}
