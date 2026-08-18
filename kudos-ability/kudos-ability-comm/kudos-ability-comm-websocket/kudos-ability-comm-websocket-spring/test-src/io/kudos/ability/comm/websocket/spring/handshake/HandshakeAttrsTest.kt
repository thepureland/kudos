package io.kudos.ability.comm.websocket.spring.handshake

import io.kudos.ability.comm.websocket.spring.FakeWebSocketSession
import org.springframework.http.HttpHeaders
import java.net.URI
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [HandshakeAttrs] query parsing.
 *
 * Query parameters are how a browser client identifies itself — the `WebSocket` constructor cannot
 * set request headers — so parsing them correctly is on the authentication path, not a convenience.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class HandshakeAttrsTest {

    @Test
    fun parsesSimpleParameters() {
        val params = HandshakeAttrs.parseQuery(URI("ws://h/ws?token=abc&tenant=acme"))

        assertEquals(listOf("abc"), params["token"])
        assertEquals(listOf("acme"), params["tenant"])
    }

    @Test
    fun decodesPercentEncodingAndPlus() {
        val params = HandshakeAttrs.parseQuery(URI("ws://h/ws?name=a%20b&other=c+d"))

        assertEquals(listOf("a b"), params["name"])
        assertEquals(listOf("c d"), params["other"], "'+' means space in a query string")
    }

    /**
     * The reason parsing goes through [URI.getRawQuery] rather than `getQuery`: the latter is already
     * decoded, so a value containing an encoded `&` or `=` — a base64 token with padding, a signed URL
     * — would be split at the wrong place and silently truncated.
     */
    @Test
    fun aValueContainingEncodedSeparators_survivesIntact() {
        val params = HandshakeAttrs.parseQuery(URI("ws://h/ws?token=a%26b%3Dc&next=1"))

        assertEquals(listOf("a&b=c"), params["token"])
        assertEquals(listOf("1"), params["next"])
    }

    @Test
    fun base64PaddingIsNotMistakenForASeparator() {
        val params = HandshakeAttrs.parseQuery(URI("ws://h/ws?t=eyJhbGciOiJIUzI1NiJ9.payload.sig%3D%3D"))

        assertEquals(listOf("eyJhbGciOiJIUzI1NiJ9.payload.sig=="), params["t"])
    }

    @Test
    fun repeatedParameters_keepEveryValueInOrder() {
        val params = HandshakeAttrs.parseQuery(URI("ws://h/ws?topic=a&topic=b&topic=c"))

        assertEquals(listOf("a", "b", "c"), params["topic"])
    }

    @Test
    fun aFlagWithoutAValue_mapsToEmptyString() {
        val params = HandshakeAttrs.parseQuery(URI("ws://h/ws?debug&token=x"))

        assertEquals(listOf(""), params["debug"])
    }

    @Test
    fun absentOrEmptyQuery_yieldsNothing() {
        assertTrue(HandshakeAttrs.parseQuery(URI("ws://h/ws")).isEmpty())
        assertTrue(HandshakeAttrs.parseQuery(URI("ws://h/ws?")).isEmpty())
        assertTrue(HandshakeAttrs.parseQuery(null).isEmpty())
    }

    @Test
    fun aMalformedEscape_isKeptLiterallyRatherThanFailingTheHandshake() {
        val params = HandshakeAttrs.parseQuery(URI("ws://h/ws?token=100%25"))

        assertEquals(listOf("100%"), params["token"])
    }

    @Test
    fun queryParam_readsThroughFromASession() {
        val session = FakeWebSocketSession(sessionUri = URI("ws://h/ws?token=abc&topic=x&topic=y"))

        assertEquals("abc", HandshakeAttrs.queryParam(session, "token"))
        assertEquals(listOf("x", "y"), HandshakeAttrs.queryParams(session, "topic"))
        assertNull(HandshakeAttrs.queryParam(session, "missing"))
    }

    @Test
    fun queryParam_prefersTheCachedAttributeOverReparsing() {
        val session = FakeWebSocketSession(sessionUri = URI("ws://h/ws?token=from-uri"))
        session.attributes[HandshakeAttrs.QUERY_PARAMS] = mapOf("token" to listOf("from-cache"))

        assertEquals("from-cache", HandshakeAttrs.queryParam(session, "token"))
    }

    /** Works without the interceptor installed — the cache is an optimisation, not a prerequisite. */
    @Test
    fun queryParam_worksWithoutTheCachedAttribute() {
        val session = FakeWebSocketSession(sessionUri = URI("ws://h/ws?token=from-uri"))

        assertEquals("from-uri", HandshakeAttrs.queryParam(session, "token"))
    }

    /**
     * Header lookup delegates to `HttpHeaders`, which is case-insensitive by contract. Worth pinning
     * because a caller reading `Authorization` must not care whether the proxy in front sent
     * `authorization`.
     */
    @Test
    fun header_readsHandshakeHeadersCaseInsensitively() {
        val session = FakeWebSocketSession(
            headers = HttpHeaders().apply {
                set("Authorization", "Bearer abc")
                set("X-Tenant-Id", "acme")
            },
        )

        assertEquals("Bearer abc", HandshakeAttrs.header(session, "Authorization"))
        assertEquals("Bearer abc", HandshakeAttrs.header(session, "authorization"))
        assertEquals("acme", HandshakeAttrs.header(session, "x-tenant-id"))
    }

    @Test
    fun header_returnsNullWhenAbsent() {
        assertNull(HandshakeAttrs.header(FakeWebSocketSession(), "Authorization"))
    }

    /** A repeated header yields its first value, matching `HttpHeaders.getFirst`. */
    @Test
    fun header_returnsTheFirstValueOfARepeatedHeader() {
        val session = FakeWebSocketSession(
            headers = HttpHeaders().apply { addAll("X-Forwarded-For", listOf("1.1.1.1", "2.2.2.2")) },
        )

        assertEquals("1.1.1.1", HandshakeAttrs.header(session, "X-Forwarded-For"))
    }

    @Test
    fun presentButBlank_isDistinguishableFromAbsent() {
        val session = FakeWebSocketSession(sessionUri = URI("ws://h/ws?token="))

        assertEquals("", HandshakeAttrs.queryParam(session, "token"))
        assertNull(HandshakeAttrs.queryParam(session, "nope"))
    }
}
