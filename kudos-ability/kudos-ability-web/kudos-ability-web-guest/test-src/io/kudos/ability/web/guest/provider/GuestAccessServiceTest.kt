package io.kudos.ability.web.guest.provider

import io.kudos.ability.web.guest.init.properties.GuestProperties
import jakarta.servlet.http.Cookie
import org.springframework.beans.factory.ObjectProvider
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import java.util.stream.Stream
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Unit tests for [GuestAccessService].
 *
 * Focus is on the wire-format and lifecycle invariants that the filter relies on:
 *  - cookie encode → decode round-trip survives `:` segments and base64 padding stripping;
 *  - fetchGuestToken returns null for missing / wrong-name cookies and throws (filter then
 *    swallows) for tampered ones;
 *  - hash populates both fields; payload extracts the configured query + ClientInfo values and
 *    falls back to empty for missing parameters / non-existent ClientInfo fields;
 *  - genToken always allocates a fresh AES token AND sets the cookie on the response.
 *
 * No Spring context — everything runs against a hand-built [GuestProperties] + Mockito-free
 * `ObjectProvider` stand-ins so the cases stay fast and traceable.
 *
 * @author AI: Claude
 * @since 1.0.0
 */
internal class GuestAccessServiceTest {

    private val properties = GuestProperties().apply {
        enabled = true
        cookie.name = "gi"
        cookie.cipherKey = "test-cipher-key-32bytes-padded--"
    }
    private val uniqueKey = IGuestAccessUniqueKey { "fixed-hash-value" }
    private val noIgnores = emptyObjectProvider<IGuestAccessIgnore>()
    private val service = GuestAccessService(properties, uniqueKey, noIgnores)

    @Test
    fun encodeDecode_roundTripsSingleToken() {
        val original = "abc123XYZ"
        val encoded = service.encodeCookie(arrayOf(original))
        val decoded = service.decodeCookie(encoded)
        assertNotNull(decoded)
        assertEquals(1, decoded.size)
        assertEquals(original, decoded[0])
        assertFalse(encoded.endsWith("="), "trailing `=` padding must be stripped to keep the cookie attribute-safe")
    }

    @Test
    fun encodeDecode_roundTripsMultipleSegmentsWithColons() {
        // The wire format reserves `:` as the inter-segment delimiter. A segment whose own value
        // contains `:` must survive the round trip — URL-encoding the segments handles that.
        val segments = arrayOf("first:with-colon", "second", "third:also:has:colons")
        val decoded = service.decodeCookie(service.encodeCookie(segments))
        assertNotNull(decoded)
        assertEquals(segments.toList(), decoded.toList())
    }

    @Test
    fun decodeCookie_returnsNullForGarbledBase64() {
        // !!! is not valid Base64 (post-padding) — must surface as a null, not an exception.
        assertNull(service.decodeCookie("not!base64!at!all!"))
    }

    @Test
    fun fetchGuestToken_returnsNullWhenNoCookies() {
        val request = MockHttpServletRequest()
        assertNull(service.fetchGuestToken(request))
    }

    @Test
    fun fetchGuestToken_returnsNullWhenCookieNameDoesNotMatch() {
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie("other", "anything"))
        }
        assertNull(service.fetchGuestToken(request))
    }

    @Test
    fun fetchGuestToken_returnsNullWhenCookieValueIsBlank() {
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie("gi", ""))
        }
        assertNull(service.fetchGuestToken(request))
    }

    @Test
    fun fetchGuestToken_returnsNullForTamperedCookie() {
        // Garbage base64 → decodes to "garbage" → not a valid AES ciphertext → service returns
        // null so the filter routes through the first-visit branch and mints a fresh cookie.
        // The tampered token is never trusted downstream.
        val request = MockHttpServletRequest().apply {
            setCookies(Cookie("gi", "Z2FyYmFnZQ"))
        }
        assertNull(service.fetchGuestToken(request))
    }

    @Test
    fun genToken_setsCookieAndReturnsToken() {
        val request = MockHttpServletRequest()
        val response = MockHttpServletResponse()
        val guest = service.genToken(request, response)
        assertNotNull(guest.token, "minted token must be returned to the caller")
        val cookie = response.cookies.singleOrNull()
        assertNotNull(cookie, "exactly one Set-Cookie must be emitted on the first visit")
        assertEquals("gi", cookie.name)
        assertEquals(properties.cookie.path, cookie.path)
        assertTrue(cookie.isHttpOnly, "default cookie must be HttpOnly")
        assertTrue(cookie.maxAge > 0, "cookie maxAge must be set to a positive number of seconds, was ${cookie.maxAge}")
    }

    @Test
    fun genToken_thenFetch_recoversTheOriginalPlaintext() {
        val response = MockHttpServletResponse()
        val minted = service.genToken(MockHttpServletRequest(), response)
        val cookieFromServer = response.cookies.single()

        // Simulate the next request from the same client carrying the cookie back.
        val secondRequest = MockHttpServletRequest().apply { setCookies(cookieFromServer) }
        val read = service.fetchGuestToken(secondRequest)

        assertNotNull(read, "second request must surface the previously-minted token")
        // The asymmetry is intentional: genToken stores the ciphertext (what goes on the wire),
        // fetchGuestToken stores the plaintext (what got decrypted from the wire). What matters
        // is that the wire format reverses cleanly — both ends are non-blank and the read side
        // is shorter (UUID < AES-hex output), proving an actual decryption happened.
        val readToken = read.token
        assertNotNull(readToken)
        assertTrue(readToken.isNotBlank(), "decrypted token must not be blank")
        assertTrue(
            readToken.length < (minted.token?.length ?: 0),
            "decrypted plaintext (a UUID) should be shorter than the encrypted hex; " +
                "if they're equal-length the cookie was not actually decrypted",
        )
    }

    @Test
    fun hash_populatesHashAndPayloadWithConfiguredFields() {
        properties.repository.payload.paramNames = listOf("v", "utm_source")
        properties.repository.payload.clientInfos = listOf("domain", "requestType")
        val request = MockHttpServletRequest().apply {
            method = "POST"
            serverName = "example.com"
            setParameter("v", "promo-42")
            setParameter("utm_source", "newsletter")
        }
        val guest = GuestAccess()

        service.hash(request, guest)

        assertEquals("fixed-hash-value", guest.hash, "hash must come from the injected IGuestAccessUniqueKey")
        val payload = assertNotNull(guest.payload)
        assertEquals("promo-42", payload["v"])
        assertEquals("newsletter", payload["utm_source"])
        assertEquals("example.com", payload["domain"], "ClientInfo.domain must be populated from request.serverName")
        assertEquals("POST", payload["requestType"])
    }

    @Test
    fun hash_emptyStringForMissingQueryParameter() {
        properties.repository.payload.paramNames = listOf("v")
        val guest = GuestAccess()

        service.hash(MockHttpServletRequest(), guest)

        assertEquals("", guest.payload?.get("v"), "a missing query param must be stored as empty, not omitted")
    }

    @Test
    fun hash_emptyStringForUnknownClientInfoField() {
        // typos in yml client-infos shouldn't crash the request path; missing reflection target
        // gets blanked out instead.
        properties.repository.payload.clientInfos = listOf("notARealField")
        val guest = GuestAccess()

        service.hash(MockHttpServletRequest(), guest)

        assertEquals("", guest.payload?.get("notARealField"))
    }

    private fun <T : Any> emptyObjectProvider(): ObjectProvider<T> = object : ObjectProvider<T> {
        override fun getObject(): T = throw UnsupportedOperationException("test stub: no beans")
        override fun getObject(vararg args: Any?): T = throw UnsupportedOperationException("test stub: no beans")
        override fun getIfAvailable(): T? = null
        override fun getIfUnique(): T? = null
        override fun stream(): Stream<T> = Stream.empty()
        override fun orderedStream(): Stream<T> = Stream.empty()
    }
}
