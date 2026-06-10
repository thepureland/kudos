package io.kudos.ability.security.jwt.resourceserver.init.properties

import org.springframework.boot.context.properties.ConfigurationProperties

/**
 * Config bindings for the JWT resource-server auto-configuration
 * ([io.kudos.ability.security.jwt.resourceserver.init.JwtResourceServerAutoConfiguration]).
 *
 * Bound from `kudos.ability.security.jwt.resource-server.*` in `application.yml`.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
@ConfigurationProperties(prefix = "kudos.ability.security.jwt.resource-server")
class JwtResourceServerProperties {

    /**
     * Master opt-in switch. Default false: depending on the module alone must never register a
     * [org.springframework.security.web.SecurityFilterChain] — that would silently hijack the
     * host app's security setup. Apps explicitly set `enabled: true` to get the default chain.
     */
    var enabled: Boolean = false

    /**
     * Ant-style path patterns that bypass authentication (`permitAll`), e.g. `/actuator/health`
     * or double-asterisk wildcard patterns under `/api/public/` and `/login/`. Empty (the default)
     * means every request requires a valid JWT — the most restrictive shape.
     */
    var permittedPaths: List<String> = emptyList()

    /**
     * Expected `iss` (issuer) claim value. Blank (the default) disables issuer validation, which
     * keeps existing deployments that mint tokens without an `iss` claim working unchanged.
     *
     * SECURITY: when several services share one signing keystore, a token minted for service A
     * verifies cryptographically on service B as well — an issuer check is the standard fence
     * against that lateral replay. Set this to the exact value the token-minting side writes
     * (`kudos.ability.security.jwt.claims.iss` when minting via `JwtParametersTool`). Tokens
     * whose `iss` claim is missing or different are rejected with 401.
     */
    var issuer: String = ""

    /**
     * Expected `aud` (audience) claim value. Blank (the default) disables audience validation.
     *
     * SECURITY: complements [issuer] in the shared-keystore scenario — even tokens from the
     * right issuer can be scoped to one consumer service. When set, the token's `aud` claim
     * (a list per RFC 7519) must contain this exact value; tokens whose `aud` is missing or
     * doesn't contain it are rejected with 401. The minting side writes the claim via
     * `kudos.ability.security.jwt.claims.aud` when using `JwtParametersTool`.
     */
    var audience: String = ""

    /** Authority-mapping knobs (see [Authorities]). */
    var authorities: Authorities = Authorities()

    class Authorities {
        /**
         * Name of the JWT claim holding the user's roles (e.g. `roles`, `groups`). Blank (the
         * default) disables role mapping — only the standard `scope` → `SCOPE_*` conversion runs.
         * When set, each claim entry surfaces as a `ROLE_*` authority so `hasRole(...)` works;
         * see [io.kudos.ability.security.jwt.resourceserver.support.KudosJwtRolesGrantedAuthoritiesConverter].
         */
        var rolesClaim: String = ""
    }
}
