package io.kudos.ability.security.jwt.resourceserver.init

import jakarta.servlet.Filter
import jakarta.servlet.http.HttpServletRequest
import org.springframework.boot.autoconfigure.AutoConfigurations
import org.springframework.boot.test.context.runner.WebApplicationContextRunner
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.web.SecurityFilterChain
import java.security.KeyPairGenerator
import java.security.interfaces.RSAPublicKey
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertSame

/**
 * Bean-method wiring tests for [JwtResourceServerAutoConfiguration.jwtResourceServerSecurityFilterChain]
 * using [WebApplicationContextRunner] (which supplies the servlet environment `@EnableWebSecurity`
 * needs so a real `HttpSecurity` prototype bean is available).
 *
 * Complements [JwtResourceServerAutoConfigurationTest] (conditional gates / property binding,
 * no chain built) and the two full-boot ITs (Nimbus decoder end-to-end). What only this class
 * covers:
 *  - A host app supplying a NON-Nimbus [JwtDecoder]: the autoconfig must NOT try to attach
 *    validators (no cast, no crash) and must still build the chain — the "custom decoder, expiry
 *    enforcement is on you" WARN branch.
 *  - Validator-chain composition combos the ITs don't boot a context for: issuer-only and
 *    audience-only configuration (ITs cover both-set and both-blank).
 *  - `@ConditionalOnMissingBean(SecurityFilterChain)` backoff: an explicit user-supplied chain
 *    bean wins, the autoconfig's bean method is never invoked.
 *  - [JwtResourceServerAutoConfiguration.getComponentName] contract.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class JwtResourceServerFilterChainWiringTest {

    // Register the autoconfig via AutoConfigurations.of so it's processed in the deferred
    // auto-configuration phase, AFTER user beans — exactly like a real boot. With plain
    // withUserConfiguration the @ConditionalOnBean(HttpSecurity) gate on the @Bean method is
    // evaluated before @EnableWebSecurity's HttpSecurityConfiguration registers the prototype.
    private val runner: WebApplicationContextRunner = WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(JwtResourceServerAutoConfiguration::class.java))
        .withPropertyValues("kudos.ability.security.jwt.resource-server.enabled=true")

    @Test
    fun nonNimbusDecoder_chainStillBuilt_withoutAttachingValidators() {
        // A host app wiring its own (non-Nimbus) decoder must not break the autoconfig: the
        // setJwtValidator call is Nimbus-specific, so the else branch logs a WARN and proceeds.
        runner
            .withUserConfiguration(NonNimbusDecoderConfig::class.java)
            .run { ctx ->
                assertNotNull(
                    ctx.getBean(SecurityFilterChain::class.java),
                    "chain must be built even when the decoder is not a NimbusJwtDecoder",
                )
            }
    }

    @Test
    fun nimbusDecoder_issuerOnly_chainBuilt() {
        // issuer set, audience blank → createDefaultWithIssuer path, no JwtAudienceValidator.
        runner
            .withUserConfiguration(NimbusDecoderConfig::class.java)
            .withPropertyValues("kudos.ability.security.jwt.resource-server.issuer=https://iss.example.com")
            .run { ctx ->
                assertNotNull(ctx.getBean(SecurityFilterChain::class.java))
            }
    }

    @Test
    fun nimbusDecoder_audienceOnly_chainBuilt() {
        // audience set, issuer blank → createDefault + JwtAudienceValidator appended.
        runner
            .withUserConfiguration(NimbusDecoderConfig::class.java)
            .withPropertyValues("kudos.ability.security.jwt.resource-server.audience=svc-a")
            .run { ctx ->
                assertNotNull(ctx.getBean(SecurityFilterChain::class.java))
            }
    }

    @Test
    fun nimbusDecoder_permittedPathsAndRolesClaim_chainBuilt() {
        // Non-empty permitted-paths + roles-claim drive the permitAll matcher branch and the
        // union'd scope+roles authorities-converter branch in a plain runner context.
        runner
            .withUserConfiguration(NimbusDecoderConfig::class.java)
            .withPropertyValues(
                "kudos.ability.security.jwt.resource-server.permitted-paths[0]=/api/public/**",
                "kudos.ability.security.jwt.resource-server.authorities.roles-claim=roles",
            )
            .run { ctx ->
                assertNotNull(ctx.getBean(SecurityFilterChain::class.java))
            }
    }

    @Test
    fun userSuppliedSecurityFilterChain_backsOffAutoconfiguredChain() {
        // @ConditionalOnMissingBean(SecurityFilterChain): an explicit chain bean in the host app
        // wins — the context must contain exactly the user's bean, not a second autoconfigured one.
        runner
            .withUserConfiguration(NimbusDecoderConfig::class.java, UserChainConfig::class.java)
            .run { ctx ->
                val chains = ctx.getBeansOfType(SecurityFilterChain::class.java)
                assertEquals(
                    1, chains.size,
                    "autoconfig must back off when the host app declares its own SecurityFilterChain",
                )
                assertSame(UserChainConfig.USER_CHAIN, chains.values.single())
            }
    }

    @Test
    fun componentName_isModuleArtifactId() {
        assertEquals(
            "kudos-ability-security-jwt-resourceserver",
            JwtResourceServerAutoConfiguration().getComponentName(),
        )
    }

    @Configuration(proxyBeanMethods = false)
    internal open class NonNimbusDecoderConfig {
        @Bean
        open fun customDecoder(): JwtDecoder = JwtDecoder { token ->
            Jwt.withTokenValue(token)
                .header("alg", "none")
                .claim("sub", "test")
                .issuedAt(Instant.now())
                .expiresAt(Instant.now().plusSeconds(60))
                .build()
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal open class NimbusDecoderConfig {
        @Bean
        open fun nimbusDecoder(): JwtDecoder {
            val keyPair = KeyPairGenerator.getInstance("RSA").apply { initialize(2048) }.generateKeyPair()
            return NimbusJwtDecoder.withPublicKey(keyPair.public as RSAPublicKey).build()
        }
    }

    @Configuration(proxyBeanMethods = false)
    internal open class UserChainConfig {
        @Bean
        open fun userChain(): SecurityFilterChain = USER_CHAIN

        companion object {
            val USER_CHAIN: SecurityFilterChain = object : SecurityFilterChain {
                override fun matches(request: HttpServletRequest): Boolean = false
                override fun getFilters(): MutableList<Filter> = mutableListOf()
            }
        }
    }
}
