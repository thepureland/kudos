package io.kudos.ability.comm.websocket.spring.handshake

import org.springframework.http.HttpStatus
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.http.server.ServletServerHttpResponse
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.handler.TextWebSocketHandler
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [KudosHandshakeInterceptor].
 *
 * The point of running guards *here* rather than rejecting later in the session factory is what the
 * client sees: a real HTTP status instead of a socket that opens and immediately closes — which every
 * WebSocket client treats as a transient fault and retries, turning a bad credential into a reconnect
 * loop. These tests pin that down, plus the fail-closed rule.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class KudosHandshakeInterceptorTest {

    private val wsHandler: WebSocketHandler = TextWebSocketHandler()

    @Test
    fun withoutGuards_theUpgradeProceeds_andQueryParamsAreCached() {
        val attributes = mutableMapOf<String, Any>()
        val (request, response) = exchange("/ws", "token=abc&topic=x")

        val proceed = KudosHandshakeInterceptor().beforeHandshake(request, response, wsHandler, attributes)

        assertTrue(proceed)
        @Suppress("UNCHECKED_CAST")
        val cached = attributes[HandshakeAttrs.QUERY_PARAMS] as Map<String, List<String>>
        assertEquals(listOf("abc"), cached["token"])
        assertEquals(listOf("x"), cached["topic"])
    }

    @Test
    fun aRejectingGuard_deniesTheUpgradeWithItsOwnStatus() {
        val servletResponse = MockHttpServletResponse()
        val (request, response) = exchange("/ws", null, servletResponse)
        val guard = IWebSocketHandshakeGuard { _, _ ->
            WebSocketHandshakeDecision.Reject(HttpStatus.UNAUTHORIZED, "missing token")
        }

        val proceed = KudosHandshakeInterceptor(listOf(guard))
            .beforeHandshake(request, response, wsHandler, mutableMapOf())

        assertFalse(proceed)
        assertEquals(401, servletResponse.status,
            "The client must be able to tell 'not allowed' from 'network hiccup'")
    }

    /** A credential or quota check that blew up must never be read as "allowed". */
    @Test
    fun aThrowingGuard_failsClosed() {
        val servletResponse = MockHttpServletResponse()
        val (request, response) = exchange("/ws", null, servletResponse)
        val guard = IWebSocketHandshakeGuard { _, _ -> error("token service unreachable") }

        val proceed = KudosHandshakeInterceptor(listOf(guard))
            .beforeHandshake(request, response, wsHandler, mutableMapOf())

        assertFalse(proceed)
        assertEquals(403, servletResponse.status)
    }

    @Test
    fun theFirstRejectionWins_andLaterGuardsDoNotRun() {
        val ran = mutableListOf<String>()
        val (request, response) = exchange("/ws", null)
        val guards = listOf(
            IWebSocketHandshakeGuard { _, _ -> ran += "first"; WebSocketHandshakeDecision.Proceed },
            IWebSocketHandshakeGuard { _, _ -> ran += "second"; WebSocketHandshakeDecision.Reject() },
            IWebSocketHandshakeGuard { _, _ -> ran += "third"; WebSocketHandshakeDecision.Proceed },
        )

        val proceed = KudosHandshakeInterceptor(guards).beforeHandshake(request, response, wsHandler, mutableMapOf())

        assertFalse(proceed)
        assertEquals(listOf("first", "second"), ran)
    }

    @Test
    fun everyGuardRuns_whenAllProceed() {
        val ran = mutableListOf<String>()
        val (request, response) = exchange("/ws", null)
        val guards = (1..3).map { n ->
            IWebSocketHandshakeGuard { _, _ -> ran += "g$n"; WebSocketHandshakeDecision.Proceed }
        }

        val proceed = KudosHandshakeInterceptor(guards).beforeHandshake(request, response, wsHandler, mutableMapOf())

        assertTrue(proceed)
        assertEquals(listOf("g1", "g2", "g3"), ran)
    }

    /**
     * A guard that already resolved a token should be able to hand the result forward instead of
     * making the session factory look it up a second time.
     */
    @Test
    fun aGuardCanPassValuesForwardThroughTheAttributes() {
        val attributes = mutableMapOf<String, Any>()
        val (request, response) = exchange("/ws", "token=abc")
        val guard = IWebSocketHandshakeGuard { _, attrs ->
            attrs["userId"] = "u-42"
            WebSocketHandshakeDecision.Proceed
        }

        KudosHandshakeInterceptor(listOf(guard)).beforeHandshake(request, response, wsHandler, attributes)

        assertEquals("u-42", attributes["userId"])
    }

    @Test
    fun aGuardSeesTheRequestUri() {
        var seen: String? = null
        val (request, response) = exchange("/ws/chat", "token=abc")
        val guard = IWebSocketHandshakeGuard { req, _ -> seen = req.uri.path; WebSocketHandshakeDecision.Proceed }

        KudosHandshakeInterceptor(listOf(guard)).beforeHandshake(request, response, wsHandler, mutableMapOf())

        assertEquals("/ws/chat", seen)
    }

    private fun exchange(
        path: String,
        query: String?,
        servletResponse: MockHttpServletResponse = MockHttpServletResponse(),
    ): Pair<ServletServerHttpRequest, ServletServerHttpResponse> {
        val servletRequest = MockHttpServletRequest("GET", path).apply { queryString = query }
        return ServletServerHttpRequest(servletRequest) to ServletServerHttpResponse(servletResponse)
    }
}
