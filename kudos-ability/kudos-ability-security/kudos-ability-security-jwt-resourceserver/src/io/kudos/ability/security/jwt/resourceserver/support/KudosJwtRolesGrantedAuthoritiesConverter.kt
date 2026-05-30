package io.kudos.ability.security.jwt.resourceserver.support

import org.springframework.core.convert.converter.Converter
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.oauth2.jwt.Jwt
import org.springframework.security.oauth2.server.resource.authentication.JwtGrantedAuthoritiesConverter

/**
 * Converts a JWT into [GrantedAuthority] objects by reading a list-valued claim and prefixing
 * each entry with `ROLE_` so Spring Security's `hasRole(...)` / `@PreAuthorize("hasRole('X')")`
 * checks just work.
 *
 * Spring's stock [JwtGrantedAuthoritiesConverter] only handles the `scope` claim and prefixes
 * with `SCOPE_` — useful for OAuth2 scope-based auth but not for the more common
 * "user has roles A, B" mental model. This converter complements it by reading whatever claim
 * name the deployment chose (e.g. `roles`, `authorities`, `groups`) and producing
 * `ROLE_A` / `ROLE_B` authorities, which `hasRole("A")` then matches by adding the prefix back.
 *
 * Claim value semantics:
 *  - `List<String>` → straight mapping (the typical shape).
 *  - `String` containing space-separated tokens → split on whitespace (matches the OAuth2 scope
 *    convention so apps can use a single string claim if their IdP doesn't emit arrays).
 *  - Missing / null / non-string → no authorities contributed (silently). Defensive: a misformed
 *    claim must not blow up the request chain; just degrade to "no roles".
 *
 * Already-`ROLE_`-prefixed entries are passed through unchanged so apps that bake the prefix
 * into their token claims stay forward-compatible. Entries are upper-cased to keep
 * `hasRole("admin")` and `hasRole("ADMIN")` from drifting apart by case.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
class KudosJwtRolesGrantedAuthoritiesConverter(
    private val rolesClaim: String,
) : Converter<Jwt, Collection<GrantedAuthority>> {

    override fun convert(source: Jwt): Collection<GrantedAuthority> {
        val raw = source.getClaim<Any?>(rolesClaim) ?: return emptyList()
        val tokens: List<String> = when (raw) {
            is Collection<*> -> raw.mapNotNull { it?.toString()?.trim()?.takeIf { s -> s.isNotEmpty() } }
            is String -> raw.split(Regex("\\s+")).filter { it.isNotEmpty() }
            else -> return emptyList()
        }
        return tokens.map { token ->
            val normalised = token.uppercase()
            val authority = if (normalised.startsWith(ROLE_PREFIX)) normalised else ROLE_PREFIX + normalised
            SimpleGrantedAuthority(authority)
        }
    }

    companion object {
        private const val ROLE_PREFIX = "ROLE_"
    }
}
