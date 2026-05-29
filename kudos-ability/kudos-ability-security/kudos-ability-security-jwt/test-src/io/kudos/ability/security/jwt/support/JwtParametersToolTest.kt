package io.kudos.ability.security.jwt.support

import io.kudos.ability.security.jwt.init.properties.SecurityJwtClaimProperties
import org.springframework.security.oauth2.jwt.JwtClaimNames
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFails
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [JwtParametersTool] — verifies that the `createDefault*` overloads correctly
 * compose claims out of the bound [SecurityJwtClaimProperties].
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class JwtParametersToolTest {

    private fun fullProps(): SecurityJwtClaimProperties = SecurityJwtClaimProperties().apply {
        iss = "test-iss"
        sub = "default-sub"
        aud = "test-aud"
        exp = 3600
        nbf = "now()"
        iat = "now()"
        jti = "uuid()"
    }

    @Test
    fun createDefault_includesAllConfiguredClaims() {
        val tool = JwtParametersTool(fullProps())
        val params = tool.createDefault()
        val claims = params.claims.claims
        assertEquals("test-iss", claims[JwtClaimNames.ISS]?.toString())
        assertEquals("default-sub", claims[JwtClaimNames.SUB])
        assertEquals(listOf("test-aud"), claims[JwtClaimNames.AUD])
        assertTrue(claims.containsKey(JwtClaimNames.EXP))
        assertTrue(claims.containsKey(JwtClaimNames.NBF))
        assertTrue(claims.containsKey(JwtClaimNames.IAT))
        assertNotEquals("", claims[JwtClaimNames.JTI], "uuid() must resolve to a non-empty jti")
    }

    @Test
    fun createDefaultWithSubject_overridesYmlSubject() {
        val tool = JwtParametersTool(fullProps())
        val params = tool.createDefault("override-sub")
        assertEquals("override-sub", params.claims.claims[JwtClaimNames.SUB])
    }

    @Test
    fun createDefaultWithSubject_rejectsBlankSubject() {
        val tool = JwtParametersTool(fullProps())
        assertFails { tool.createDefault("") }
        assertFails { tool.createDefault("   ") }
    }

    @Test
    fun createDefaultWithMap_mergesCustomClaimsOverDefaults() {
        val tool = JwtParametersTool(fullProps())
        val params = tool.createDefault(mapOf("roles" to listOf("admin", "user"), "iss" to "overridden-iss"))
        val claims = params.claims.claims
        assertEquals(listOf("admin", "user"), claims["roles"])
        // Custom map can clobber default claims — confirms the merge order is defaults first,
        // custom last (so custom wins).
        assertEquals("overridden-iss", claims[JwtClaimNames.ISS]?.toString())
    }

    @Test
    fun createDefaultWithSubjectAndMap_combinesBoth() {
        val tool = JwtParametersTool(fullProps())
        val params = tool.createDefault("explicit-sub", mapOf("scope" to "read write"))
        val claims = params.claims.claims
        assertEquals("explicit-sub", claims[JwtClaimNames.SUB])
        assertEquals("read write", claims["scope"])
    }

    @Test
    fun emptyProps_produceMinimalClaimsButNoCrash() {
        // Zero-config JwtParametersTool must still produce a usable JwtEncoderParameters — apps
        // that supply per-call data should not be forced to set yml defaults first.
        val tool = JwtParametersTool(SecurityJwtClaimProperties())
        val params = tool.createDefault("explicit-only")
        assertEquals("explicit-only", params.claims.claims[JwtClaimNames.SUB])
        assertNull(params.claims.claims[JwtClaimNames.EXP], "no exp default, no exp emitted")
        assertNull(params.claims.claims[JwtClaimNames.ISS])
    }
}
