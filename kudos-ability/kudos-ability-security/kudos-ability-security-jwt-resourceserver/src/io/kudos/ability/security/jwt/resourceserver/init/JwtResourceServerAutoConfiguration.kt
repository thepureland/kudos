package io.kudos.ability.security.jwt.resourceserver.init

import io.kudos.ability.security.jwt.resourceserver.init.properties.JwtResourceServerProperties
import io.kudos.ability.security.jwt.resourceserver.support.KudosJwtRolesGrantedAuthoritiesConverter
import io.kudos.ability.security.jwt.support.JwtExpValidator
import io.kudos.context.config.YamlPropertySourceFactory
import io.kudos.context.init.IComponentInitializer
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.boot.context.properties.EnableConfigurationProperties
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.context.annotation.PropertySource
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationConverter
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter
import org.springframework.security.web.SecurityFilterChain

/**
 * Auto-configuration that wires a default OAuth2 Resource Server [SecurityFilterChain] backed by
 * the JWT module's [JwtDecoder] + [JwtExpValidator].
 *
 * Pulls together pieces that would otherwise be hand-wired by every app:
 *  - Uses the [JwtDecoder] bean published by `kudos-ability-security-jwt` (RS256 + PKCS12 keystore).
 *  - Layers [JwtExpValidator] onto the decoder so token expiry is enforced even if the keystore's
 *    JWT doesn't ship an explicit issuer/audience validator.
 *  - Builds a [SecurityFilterChain] with `oauth2ResourceServer.jwt(decoder)` and a default
 *    authorization shape: configured [JwtResourceServerProperties.permittedPaths] are public,
 *    everything else requires authentication.
 *  - Disables CSRF — typical for stateless REST APIs behind JWT, where the token in the
 *    Authorization header is itself the CSRF defense and double-submit cookies aren't relevant.
 *
 * Gating:
 *  - [ConditionalOnClass] on [HttpSecurity] / [SecurityFilterChain]: only fires when
 *    spring-security-config is on the classpath (always true via this module's own dep, but
 *    keeps the autoconfig defensive against future transitive excludes).
 *  - [ConditionalOnProperty] `kudos.ability.security.jwt.resource-server.enabled` (default false):
 *    pure opt-in so the host app's existing `SecurityFilterChain` isn't accidentally clobbered.
 *  - [ConditionalOnMissingBean] [SecurityFilterChain]: even with the flag on, an explicit
 *    `@Bean SecurityFilterChain` in the host app wins.
 *
 * For richer needs (role-based authorization, custom converters from JWT claims to
 * `Authentication`, multi-issuer support, etc.) apps disable this autoconfig and declare their
 * own `SecurityFilterChain` — this module is for the common "any authenticated user can hit any
 * endpoint, with explicit public-path exceptions" shape.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@Configuration(proxyBeanMethods = false)
@EnableWebSecurity
@ConditionalOnClass(HttpSecurity::class, SecurityFilterChain::class)
@ConditionalOnProperty(
    prefix = "kudos.ability.security.jwt.resource-server",
    name = ["enabled"],
    havingValue = "true",
)
@EnableConfigurationProperties(JwtResourceServerProperties::class)
@PropertySource(
    value = ["classpath:kudos-ability-security-jwt-resourceserver.yml"],
    factory = YamlPropertySourceFactory::class,
)
open class JwtResourceServerAutoConfiguration : IComponentInitializer {

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain::class)
    @ConditionalOnBean(value = [HttpSecurity::class, JwtDecoder::class])
    open fun jwtResourceServerSecurityFilterChain(
        http: HttpSecurity,
        decoder: JwtDecoder,
        properties: JwtResourceServerProperties,
    ): SecurityFilterChain {
        // Compose the decoder's validators: keep whatever JwtDecoder shipped (issuer / audience
        // defaults), then AND our exp-only validator on top so expiry-rejection is uniform across
        // all kudos-issued tokens. NimbusJwtDecoder is the type the JWT module ships; if a host
        // app supplied a different impl, we don't try to attach JwtExpValidator (the cast would
        // fail noisily) — they're presumably wiring their own validators anyway.
        if (decoder is NimbusJwtDecoder) {
            decoder.setJwtValidator(
                org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator(
                    org.springframework.security.oauth2.jwt.JwtValidators.createDefault(),
                    JwtExpValidator(),
                ),
            )
        }

        val permitted = properties.permittedPaths.toTypedArray()
        // Authority extraction. Stock JwtGrantedAuthoritiesConverter handles `scope` → `SCOPE_*`.
        // When the deployment also wants `roles` → `ROLE_*` for `hasRole(...)` checks, layer the
        // kudos converter on top — its output union'd with the scope converter via a wrapper.
        val rolesClaim = properties.authorities.rolesClaim.takeIf { it.isNotBlank() }
        val authConverter = JwtAuthenticationConverter().apply {
            val scopeConverter = JwtGrantedAuthoritiesConverter()
            if (rolesClaim != null) {
                val rolesConverter = KudosJwtRolesGrantedAuthoritiesConverter(rolesClaim)
                setJwtGrantedAuthoritiesConverter { jwt ->
                    val out = mutableListOf<org.springframework.security.core.GrantedAuthority>()
                    scopeConverter.convert(jwt)?.let { out += it }
                    out += rolesConverter.convert(jwt)
                    out
                }
            } else {
                setJwtGrantedAuthoritiesConverter(scopeConverter)
            }
        }

        return http
            .csrf { it.disable() }
            .authorizeHttpRequests { auth ->
                if (permitted.isNotEmpty()) {
                    auth.requestMatchers(*permitted).permitAll()
                }
                auth.anyRequest().authenticated()
            }
            .oauth2ResourceServer { rs ->
                rs.jwt { jwt ->
                    jwt.decoder(decoder)
                    jwt.jwtAuthenticationConverter(authConverter)
                }
            }
            .build()
    }

    override fun getComponentName() = "kudos-ability-security-jwt-resourceserver"
}
