package io.kudos.ability.security.jwt.resourceserver

import io.kudos.ability.security.jwt.resourceserver.JwtResourceServerFilterChainIT.Companion.dynamicProperties
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
 * End-to-end integration test for the JWT OAuth2 Resource Server filter chain.
 *
 * Boots the full Spring Boot context: kudos-ability-security-jwt provides the JwtEncoder /
 * JwtDecoder, this module's autoconfig provides the SecurityFilterChain, and a TestConfiguration
 * adds a tiny REST controller with two endpoints (`/private`, `/api/public/echo`). MockMvc then
 * verifies the filter chain's authorization shape:
 *  - `/private` with no Authorization → 401
 *  - `/private` with a valid JWT (signed by the test keystore) → 200
 *  - `/private` with an expired JWT → 401 (JwtExpValidator fires)
 *  - `/api/public/echo` (listed in permitted-paths) with no Authorization → 200
 *  - `/api/public/echo` with a valid JWT → 200 (permitted bypasses auth check but doesn't reject)
 *
 * The keystore is generated programmatically at `@DynamicPropertySource` setup time via Bouncy
 * Castle — same pattern as the parent JWT module's tests, no binary blob in the repo.
 *
 * Why a full `@SpringBootTest` instead of ApplicationContextRunner:
 *  - SecurityFilterChain isn't exercised by Spring's mock context unless the full security
 *    infrastructure (HttpSecurityConfiguration / WebSecurityConfiguration) is wired, which
 *    requires `@EnableWebSecurity` + a real servlet container or MockMvc.
 *  - `@AutoConfigureMockMvc` with `apply(springSecurity())` is the canonical way to test
 *    SecurityFilterChain end-to-end without an HTTP server.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
@EnableKudosTest
@AutoConfigureMockMvc
@Import(JwtResourceServerFilterChainIT.TestControllers::class)
internal class JwtResourceServerFilterChainIT {

    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var encoder: JwtEncoder

    @Test
    fun privateEndpoint_withoutAuthorizationHeader_returns401() {
        mockMvc.get("/private").andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun privateEndpoint_withValidJwt_returns200() {
        val token = mintToken(subject = "alice", ttl = Duration.ofMinutes(5))
        mockMvc.get("/private") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
            content {
                contentTypeCompatibleWith(MediaType.TEXT_PLAIN)
                string("hello alice")
            }
        }
    }

    @Test
    fun privateEndpoint_withExpiredJwt_returns401() {
        // mintToken with a negative TTL produces a token whose exp is already in the past.
        // JwtExpValidator (wired by JwtResourceServerAutoConfiguration) should reject it.
        val expired = mintToken(subject = "alice", ttl = Duration.ofSeconds(-30))
        mockMvc.get("/private") {
            header("Authorization", "Bearer $expired")
        }.andExpect {
            status { isUnauthorized() }
        }
    }

    @Test
    fun publicEndpoint_withoutAuthorizationHeader_returns200() {
        // The /api/public/** pattern is in permitted-paths; the filter chain must NOT require a
        // token. This is the typical health-check / public-docs / login-issue endpoint shape.
        mockMvc.get("/api/public/echo").andExpect {
            status { isOk() }
            content { string("public ok") }
        }
    }

    @Test
    fun publicEndpoint_withValidJwt_returns200() {
        // permitAll() permits ALL requests on these paths, regardless of whether they carry a
        // valid token. Apps sometimes worry the chain rejects authenticated access to public
        // endpoints; lock in that it doesn't.
        val token = mintToken(subject = "alice", ttl = Duration.ofMinutes(5))
        mockMvc.get("/api/public/echo") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }
    }

    private fun mintToken(
        subject: String,
        ttl: Duration,
        roles: List<String> = emptyList(),
    ): String {
        // Backdate iat to 1 hour ago so even an "already-expired" token (negative ttl) still
        // satisfies the spec contract `exp > iat` — JwtClaimsSet.Builder rejects exp <= iat.
        val iat = Instant.now().minus(Duration.ofHours(1))
        val builder = JwtClaimsSet.builder()
            .subject(subject)
            .issuedAt(iat)
            .expiresAt(iat.plus(Duration.ofHours(1)).plus(ttl))
            .id(UUID.randomUUID().toString())
        if (roles.isNotEmpty()) builder.claim("roles", roles)
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(SignatureAlgorithm.RS256).build(), builder.build()))
            .tokenValue
    }

    @Test
    fun authoritiesEndpoint_withRolesClaim_surfacesRolePrefixedAuthorities() {
        // Proves the KudosJwtRolesGrantedAuthoritiesConverter is wired into the filter chain via
        // the autoconfig: a token whose `roles` claim says ["ADMIN", "AUDITOR"] produces an
        // Authentication whose authorities contain ROLE_ADMIN + ROLE_AUDITOR.
        val token = mintToken(subject = "alice", ttl = Duration.ofMinutes(5), roles = listOf("ADMIN", "AUDITOR"))
        val response = mockMvc.get("/private/authorities") {
            header("Authorization", "Bearer $token")
        }.andExpect {
            status { isOk() }
        }.andReturn().response.contentAsString
        val authorities = response.split(",").toSet()
        kotlin.test.assertTrue(
            "ROLE_ADMIN" in authorities,
            "roles=[ADMIN, ...] must surface as ROLE_ADMIN authority; got: $authorities",
        )
        kotlin.test.assertTrue("ROLE_AUDITOR" in authorities)
    }

    @Test
    fun authoritiesEndpoint_withoutRolesClaim_hasNoRoleAuthorities() {
        // Defensive: a token without the roles claim must NOT produce phantom ROLE_* authorities.
        val token = mintToken(subject = "alice", ttl = Duration.ofMinutes(5)) // no roles
        val response = mockMvc.get("/private/authorities") {
            header("Authorization", "Bearer $token")
        }.andReturn().response.contentAsString
        val rolePrefixed = response.split(",").filter { it.startsWith("ROLE_") }
        kotlin.test.assertEquals(
            emptyList(), rolePrefixed,
            "no roles claim → no ROLE_* authorities; got: $rolePrefixed",
        )
    }

    /** Two endpoints — one private (default secured), one public (permitted-paths bypass). */
    @TestConfiguration
    internal open class TestControllers {
        @Bean
        open fun privateController(): PrivateController = PrivateController()

        @Bean
        open fun publicController(): PublicController = PublicController()
    }

    @RestController
    internal class PrivateController {
        @GetMapping("/private", produces = [MediaType.TEXT_PLAIN_VALUE])
        fun private(@org.springframework.security.core.annotation.AuthenticationPrincipal jwt: org.springframework.security.oauth2.jwt.Jwt): String =
            "hello ${jwt.subject}"

        @GetMapping("/private/authorities", produces = [MediaType.TEXT_PLAIN_VALUE])
        fun authorities(authentication: org.springframework.security.core.Authentication): String =
            authentication.authorities.joinToString(",") { it.authority ?: "" }
    }

    @RestController
    internal class PublicController {
        @GetMapping("/api/public/echo", produces = [MediaType.TEXT_PLAIN_VALUE])
        fun echo(): String = "public ok"
    }

    companion object {
        private const val KEYSTORE_ALIAS = "kudos-jwt-it-key"
        private const val KEYSTORE_PASSWORD = "kudos-it-pwd"

        // Stored as a JVM-static so the dynamic-property lambda can read it; lazy so the BC
        // keystore generator runs only once per test JVM regardless of how many `@DynamicPropertySource`
        // callbacks Spring fires.
        private val keystorePath: String by lazy { writeKeystore().absolutePath }

        private fun writeKeystore(): File {
            if (Security.getProvider(BouncyCastleProvider.PROVIDER_NAME) == null) {
                Security.addProvider(BouncyCastleProvider())
            }
            val tempDir = Files.createTempDirectory("kudos-jwt-it").toFile().apply { deleteOnExit() }
            val keystoreFile = File(tempDir, "test-jwt.p12").apply { deleteOnExit() }

            val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            val notBefore = Date()
            val notAfter = Date(notBefore.time + Duration.ofDays(7).toMillis())
            val subject = X500Principal("CN=kudos-jwt-it")
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
            registry.add("kudos.ability.security.jwt.resource-server.permitted-paths[0]") { "/api/public/**" }
            registry.add("kudos.ability.security.jwt.resource-server.authorities.roles-claim") { "roles" }
        }
    }
}
