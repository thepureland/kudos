package io.kudos.ability.file.minio.client

import com.sun.net.httpserver.HttpServer
import io.kudos.ability.file.common.auth.AccessTokenServerParam
import io.kudos.ability.file.minio.init.properties.AccessTokenServerProperties
import io.kudos.ability.file.minio.init.properties.MinioProperties
import java.net.InetSocketAddress
import java.net.ServerSocket
import java.nio.charset.StandardCharsets
import java.security.ProviderException
import java.util.Base64
import java.util.concurrent.ConcurrentHashMap
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Pure-logic tests for [AccessTokenMinioClientBuilder] (OAuth2 token -> MinIO STS).
 *
 * A throwaway JDK [HttpServer] bound to 127.0.0.1:<random port> plays the OAuth2 token
 * endpoint, so the whole flow is deterministic and never touches the real network:
 * - accessToken(): happy path — POSTs grant_type, forwards the business token under the
 *   configured header name, sends Basic clientId:clientSecret, and deserializes the JSON
 *   answer into a Minio [io.minio.credentials.Jwt].
 * - accessToken(): non-2xx answer -> ProviderException naming the HTTP status, and the
 *   response body must NOT leak into the message (error pages may echo request headers).
 * - accessToken(): connection refused -> IOException wrapped in ProviderException.
 * - accessToken(): missing grant type / endpoint / header name / header value fail fast.
 * - build(): assembles a MinioClient offline (the WebIdentityProvider fetches lazily).
 * - build(): missing endpoint fails fast.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AccessTokenMinioClientBuilderTest {

    /** Exposes the protected accessToken() for direct testing. */
    private class TestableBuilder : AccessTokenMinioClientBuilder() {
        fun fetchToken(param: AccessTokenServerParam) = accessToken(param)
    }

    private lateinit var server: HttpServer

    /** Headers captured from the last request hitting /token, for behavioral assertions. */
    private val capturedHeaders = ConcurrentHashMap<String, String>()

    private var grantTypeBody: String = ""

    @BeforeTest
    fun startServer() {
        server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        server.createContext("/token") { exchange ->
            exchange.requestHeaders.forEach { (k, v) -> capturedHeaders[k.lowercase()] = v.joinToString(",") }
            grantTypeBody = String(exchange.requestBody.readAllBytes(), StandardCharsets.UTF_8)
            val body = """{"access_token":"unit-test-token","expires_in":1234,"token_type":"Bearer","scope":"s3"}"""
                .toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(200, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.createContext("/unauthorized") { exchange ->
            val body = "secret-echo-of-request".toByteArray(StandardCharsets.UTF_8)
            exchange.sendResponseHeaders(401, body.size.toLong())
            exchange.responseBody.use { it.write(body) }
        }
        server.start()
    }

    @AfterTest
    fun stopServer() {
        server.stop(0)
    }

    private fun endpointBase() = "http://127.0.0.1:${server.address.port}"

    private fun tokenServerProperties(endpoint: String? = "${endpointBase()}/token") =
        AccessTokenServerProperties().also {
            it.authorizationGrantType = "client_credentials"
            it.clientId = "test-client"
            it.clientSecret = "test-secret"
            it.endpoint = endpoint
            it.headerName = "X-Kudos-Auth"
        }

    private fun builder(tokenProps: AccessTokenServerProperties = tokenServerProperties()): TestableBuilder {
        val b = TestableBuilder()
        b.setMinioProperties(MinioProperties().also { it.endpoint = "http://127.0.0.1:9000" })
        b.setAccessTokenServerProperties(tokenProps)
        return b
    }

    private fun param(headerValue: String? = "Bearer biz-token") =
        AccessTokenServerParam().also { it.headerValue = headerValue }

    @Test
    fun accessTokenParsesJwtFromTokenEndpoint() {
        val jwt = builder().fetchToken(param())

        assertNotNull(jwt)
        assertEquals("unit-test-token", jwt.token())
        assertEquals(1234, jwt.expiry())
    }

    @Test
    fun accessTokenForwardsBusinessTokenAndBasicCredentials() {
        builder().fetchToken(param("Bearer pass-through"))

        // The business token must be forwarded verbatim under the configured header name.
        assertEquals("Bearer pass-through", capturedHeaders["x-kudos-auth"])
        // The Authorization header must be Basic base64(clientId:clientSecret).
        val expectedBasic = Base64.getEncoder()
            .encodeToString("test-client:test-secret".toByteArray(StandardCharsets.UTF_8))
        assertEquals("Basic $expectedBasic", capturedHeaders["authorization"])
        // The form body must carry the configured grant type.
        assertTrue(grantTypeBody.contains("grant_type=client_credentials"))
    }

    @Test
    fun accessTokenFailsWithHttpStatusButWithoutResponseBodyOnNon2xx() {
        val b = builder(tokenServerProperties(endpoint = "${endpointBase()}/unauthorized"))

        val e = assertFailsWith<ProviderException> { b.fetchToken(param()) }
        assertTrue(e.message!!.contains("401"), "message should name the HTTP status: ${e.message}")
        // The body may echo request headers (i.e. the business token); it must not leak.
        assertTrue(!e.message!!.contains("secret-echo-of-request"))
    }

    @Test
    fun accessTokenWrapsConnectionFailureInProviderException() {
        // A port that was just opened and closed again: nothing is listening there.
        val deadPort = ServerSocket(0).use { it.localPort }
        val b = builder(tokenServerProperties(endpoint = "http://127.0.0.1:$deadPort/token"))

        val e = assertFailsWith<ProviderException> { b.fetchToken(param()) }
        assertNotNull(e.cause, "the original IOException must be preserved as the cause")
    }

    @Test
    fun accessTokenFailsFastWhenGrantTypeMissing() {
        val props = tokenServerProperties().also { it.authorizationGrantType = null }

        val e = assertFailsWith<IllegalArgumentException> { builder(props).fetchToken(param()) }
        assertTrue(e.message!!.contains("authorizationGrantType"))
    }

    @Test
    fun accessTokenFailsFastWhenEndpointMissing() {
        val e = assertFailsWith<IllegalArgumentException> {
            builder(tokenServerProperties(endpoint = null)).fetchToken(param())
        }
        assertTrue(e.message!!.contains("endpoint"))
    }

    @Test
    fun accessTokenFailsFastWhenHeaderNameMissing() {
        val props = tokenServerProperties().also { it.headerName = null }

        val e = assertFailsWith<IllegalArgumentException> { builder(props).fetchToken(param()) }
        assertTrue(e.message!!.contains("headerName"))
    }

    @Test
    fun accessTokenFailsFastWhenHeaderValueMissing() {
        val e = assertFailsWith<IllegalArgumentException> {
            builder().fetchToken(param(headerValue = null))
        }
        assertTrue(e.message!!.contains("headerValue"))
    }

    @Test
    fun buildAssemblesClientOffline() {
        val b = builder()
        b.setAuthServerParam(param())

        // WebIdentityProvider fetches credentials lazily, so build() itself is offline.
        assertNotNull(b.build())
    }

    @Test
    fun buildFailsFastWhenMinioEndpointMissing() {
        val b = TestableBuilder()
        b.setMinioProperties(MinioProperties()) // endpoint left null
        b.setAccessTokenServerProperties(tokenServerProperties())
        b.setAuthServerParam(param())

        val e = assertFailsWith<IllegalArgumentException> { b.build() }
        assertTrue(e.message!!.contains("endpoint"))
    }

}
