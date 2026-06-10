package io.kudos.ability.security.jwt.resourceserver.init

import io.kudos.ability.security.jwt.resourceserver.init.properties.JwtResourceServerProperties
import io.kudos.ability.security.jwt.resourceserver.support.JwtAudienceValidator
import io.kudos.ability.security.jwt.resourceserver.support.KudosJwtRolesGrantedAuthoritiesConverter
import io.kudos.ability.security.jwt.support.JwtExpValidator
import io.kudos.base.logger.LogFactory
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
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator
import org.springframework.security.oauth2.core.OAuth2TokenValidator
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.jwt.JwtDecoder
import org.springframework.security.oauth2.jwt.JwtValidators
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
 *  - Optionally pins `iss` / `aud`: when [JwtResourceServerProperties.issuer] or
 *    [JwtResourceServerProperties.audience] are configured, the validator chain additionally
 *    rejects tokens minted for another service off the same shared keystore (lateral replay).
 *    Both blank (the backward-compatible default) skips the checks and logs a WARN once.
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

    private val log = LogFactory.getLog(this::class)

    @Bean
    @ConditionalOnMissingBean(SecurityFilterChain::class)
    @ConditionalOnBean(value = [HttpSecurity::class, JwtDecoder::class])
    open fun jwtResourceServerSecurityFilterChain(
        http: HttpSecurity,
        decoder: JwtDecoder,
        properties: JwtResourceServerProperties,
    ): SecurityFilterChain {
        // Compose the decoder's validator chain: base timestamp validation, our exp-only
        // validator (uniform expiry rejection across all kudos-issued tokens), then optional
        // issuer / audience pinning from properties. NimbusJwtDecoder is the type the JWT module
        // ships; if a host app supplied a different impl, we don't try to attach validators —
        // they're presumably wiring their own anyway (logged below so it's not silent).
        if (decoder is NimbusJwtDecoder) {
            val issuer = properties.issuer.takeIf { it.isNotBlank() }
            val audience = properties.audience.takeIf { it.isNotBlank() }
            val validators = mutableListOf<OAuth2TokenValidator<Jwt>>(
                // createDefaultWithIssuer already bundles the default timestamp validator plus
                // the issuer check — use it INSTEAD OF createDefault, never both, so the default
                // validators aren't stacked twice.
                if (issuer != null) JwtValidators.createDefaultWithIssuer(issuer)
                else JwtValidators.createDefault(),
                JwtExpValidator(),
            )
            if (audience != null) {
                validators += JwtAudienceValidator(audience)
            }
            decoder.setJwtValidator(DelegatingOAuth2TokenValidator(validators))
            if (issuer == null && audience == null) {
                // SECURITY observability: signature verification alone doesn't bind a token to
                // THIS service. With several services sharing one keystore, a token minted for
                // service A replays verbatim on service B unless iss / aud are pinned.
                log.warn(
                    "JWT issuer/audience validation is DISABLED (kudos.ability.security.jwt." +
                        "resource-server.issuer / .audience are both blank). If multiple services " +
                        "share the same signing keystore, tokens issued for one service can be " +
                        "replayed against this one. Configure issuer and audience to prevent " +
                        "lateral token replay.",
                )
            }
        } else {
            // Observability: make the silent "exp validator not attached" branch visible — apps
            // wiring a custom decoder should know kudos's uniform expiry rejection is on them.
            log.warn(
                "JwtDecoder bean is {0}, not NimbusJwtDecoder; JwtExpValidator was NOT attached. " +
                    "Ensure the custom decoder enforces token expiry itself.",
                decoder::class.qualifiedName,
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
                    val out = mutableListOf<GrantedAuthority>()
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
