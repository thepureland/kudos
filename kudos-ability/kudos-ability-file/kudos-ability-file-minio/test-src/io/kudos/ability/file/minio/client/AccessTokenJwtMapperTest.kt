package io.kudos.ability.file.minio.client

import com.fasterxml.jackson.annotation.JsonAutoDetect
import io.minio.credentials.Jwt
import tools.jackson.databind.DeserializationFeature
import tools.jackson.databind.json.JsonMapper
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Locks in the deserialization behavior of the Jackson 3 mapper configuration in
 * [AccessTokenMinioClientBuilder] against [io.minio.credentials.Jwt].
 *
 * Background: Minio's `Jwt` is an immutable class (two private final fields, no
 * no-arg constructor); Jackson finds no entry point via getters/setters by default.
 * The key to making OIDC token parsing work is the
 * `changeDefaultVisibility { withFieldVisibility(ANY) }` on the mapper — it writes
 * the private fields directly.
 *
 * After switching from Jackson 2 `ObjectMapper().setVisibility(...)` to Jackson 3
 * `JsonMapper.builder().changeDefaultVisibility(...)`, the path is in theory
 * equivalent, but Jackson 3 has nearly rewritten the builder/visibility API, so
 * equivalence cannot be taken at face value. Any regression (builder API renamed,
 * visibility-resolution semantics changed, FAIL_ON_UNKNOWN default changed) would
 * cause this STS integration path to fail only after production startup. This test
 * locks it down at compile time and unit-test time.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class AccessTokenJwtMapperTest {

    /** The exact same mapper construction used inside AccessTokenMinioClientBuilder.accessToken(). */
    private val mapper = JsonMapper.builder()
        .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES)
        .changeDefaultVisibility { it.withFieldVisibility(JsonAutoDetect.Visibility.ANY) }
        .build()

    @Test
    fun deserializesMinioJwtFromStandardOAuthResponse() {
        // Jwt's two fields are bound via @ConstructorProperties + @JsonProperty to the
        // OAuth2 standard keys access_token / expires_in (see the io.minio.credentials.Jwt bytecode).
        val json = """{"access_token":"eyJhbGciOiJIUzI1NiJ9.payload.sig","expires_in":3600}"""

        val jwt = mapper.readValue(json, Jwt::class.java)

        assertNotNull(jwt)
        assertEquals("eyJhbGciOiJIUzI1NiJ9.payload.sig", jwt.token())
        assertEquals(3600, jwt.expiry())
    }

    @Test
    fun ignoresUnknownPropertiesFromTokenServer() {
        // Real OIDC token servers also return extra fields like refresh_token / scope / token_type.
        // The mapper must let them through, otherwise Jwt deserialization would throw because of FAIL_ON_UNKNOWN_PROPERTIES.
        val json = """
            {
              "access_token": "abc",
              "expires_in": 1800,
              "refresh_token": "y",
              "scope": "read write",
              "token_type": "Bearer"
            }
        """.trimIndent()

        val jwt = mapper.readValue(json, Jwt::class.java)

        assertEquals("abc", jwt.token())
        assertEquals(1800, jwt.expiry())
    }
}
