package io.kudos.ability.security.jwt.resourceserver.support

import org.springframework.security.oauth2.jwt.Jwt
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Unit tests for [KudosJwtRolesGrantedAuthoritiesConverter].
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosJwtRolesGrantedAuthoritiesConverterTest {

    private val converter = KudosJwtRolesGrantedAuthoritiesConverter("roles")

    @Test
    fun missingClaim_yieldsEmptyAuthorities() {
        val jwt = newJwt(emptyMap())
        assertEquals(emptyList(), converter.convert(jwt).toList())
    }

    @Test
    fun listClaim_eachEntryBecomesRolePrefixedAuthority() {
        val jwt = newJwt(mapOf("roles" to listOf("admin", "user")))
        val authNames = converter.convert(jwt).map { it.authority }.toSet()
        assertEquals(setOf("ROLE_ADMIN", "ROLE_USER"), authNames)
    }

    @Test
    fun stringClaim_splitsOnWhitespace() {
        // Some IdPs emit space-separated strings (mirroring OAuth2 scope conventions).
        val jwt = newJwt(mapOf("roles" to "admin user editor"))
        val authNames = converter.convert(jwt).map { it.authority }.toSet()
        assertEquals(setOf("ROLE_ADMIN", "ROLE_USER", "ROLE_EDITOR"), authNames)
    }

    @Test
    fun alreadyPrefixedEntries_passThroughWithoutDoublePrefix() {
        // Apps that bake the prefix into the token claim must NOT end up with ROLE_ROLE_X.
        val jwt = newJwt(mapOf("roles" to listOf("ROLE_ADMIN", "user")))
        val authNames = converter.convert(jwt).map { it.authority }.toSet()
        assertEquals(setOf("ROLE_ADMIN", "ROLE_USER"), authNames)
    }

    @Test
    fun mixedCase_isNormalisedToUpper() {
        val jwt = newJwt(mapOf("roles" to listOf("Admin", "uSeR")))
        val authNames = converter.convert(jwt).map { it.authority }.toSet()
        assertEquals(setOf("ROLE_ADMIN", "ROLE_USER"), authNames)
    }

    @Test
    fun blankEntries_areDropped() {
        // Defensive: a trailing comma or stray empty string in the claim must not produce
        // ROLE_ (empty role name) authorities.
        val jwt = newJwt(mapOf("roles" to listOf("admin", "", "  ", "user")))
        val authNames = converter.convert(jwt).map { it.authority }.toSet()
        assertEquals(setOf("ROLE_ADMIN", "ROLE_USER"), authNames)
    }

    @Test
    fun nonStringClaimValue_yieldsEmpty_doesNotThrow() {
        // An IdP that emits roles as a number (or any non-string/non-collection value) must NOT
        // crash the request chain — it just degrades to "no roles". Surprise nulls / exceptions
        // here propagate as 500 errors on every request, which is much worse than "no auth".
        val jwt = newJwt(mapOf("roles" to 42))
        assertTrue(converter.convert(jwt).isEmpty())
    }

    @Test
    fun differentClaimName_isHonored() {
        val custom = KudosJwtRolesGrantedAuthoritiesConverter("groups")
        val jwt = newJwt(mapOf("groups" to listOf("ops"), "roles" to listOf("ignored")))
        val authNames = custom.convert(jwt).map { it.authority }.toSet()
        assertEquals(setOf("ROLE_OPS"), authNames)
    }

    private fun newJwt(claims: Map<String, Any?>): Jwt {
        val builder = Jwt.withTokenValue("test")
            .header("alg", "RS256")
            .issuedAt(Instant.now())
            .expiresAt(Instant.now().plusSeconds(60))
            .subject("test-sub")
        if (claims.isEmpty()) {
            builder.claim("placeholder", "x") // builder requires at least one claim
        } else {
            claims.forEach { (k, v) -> if (v != null) builder.claim(k, v) }
        }
        return builder.build()
    }
}
