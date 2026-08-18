package io.kudos.ability.comm.websocket.spring.handshake

import org.springframework.web.socket.WebSocketSession
import java.net.URI
import java.net.URLDecoder
import java.nio.charset.StandardCharsets

/**
 * Access to what the handshake knew, from a [WebSocketSession].
 *
 * Deliberately thin. Spring's [WebSocketSession] already exposes the handshake headers
 * ([WebSocketSession.getHandshakeHeaders]), the authenticated principal
 * ([WebSocketSession.getPrincipal]), the remote address and the request URI, so copying those into
 * the attribute map — as a hand-rolled handshake interceptor usually does — buys nothing and only
 * creates a second, staler source of truth.
 *
 * What Spring does *not* give you is parsed query parameters: [WebSocketSession.getUri] hands back a
 * [URI] whose query is one raw string. Since a token, a device id or a protocol version arriving as
 * `?token=...` is the overwhelmingly common way a browser client identifies itself — `WebSocket` in
 * the browser cannot set request headers — that parsing is the one thing worth providing here.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object HandshakeAttrs {

    /**
     * Attribute key under which [KudosHandshakeInterceptor] caches the parsed query parameters.
     *
     * The accessors below fall back to parsing the URI when it is absent, so they work whether or not
     * that interceptor is installed; the attribute only avoids re-parsing.
     */
    const val QUERY_PARAMS: String = "_KUDOS_WS_QUERY_PARAMS_"

    /**
     * The first value of query parameter [name], or `null` when absent.
     *
     * An empty value (`?token=`) is returned as an empty string, not `null` — "present but blank" and
     * "not sent at all" are different client bugs and a caller may want to tell them apart. Use
     * `?.takeIf { it.isNotBlank() }` when they should be treated alike.
     */
    fun queryParam(session: WebSocketSession, name: String): String? = queryParams(session, name).firstOrNull()

    /** Every value of query parameter [name], in the order they appeared; empty when absent. */
    fun queryParams(session: WebSocketSession, name: String): List<String> = allQueryParams(session)[name].orEmpty()

    /**
     * All query parameters, percent-decoded.
     *
     * Reads the cached [QUERY_PARAMS] attribute when present, otherwise parses [WebSocketSession.getUri].
     */
    @Suppress("UNCHECKED_CAST")
    fun allQueryParams(session: WebSocketSession): Map<String, List<String>> {
        val cached = session.attributes[QUERY_PARAMS]
        if (cached is Map<*, *>) return cached as Map<String, List<String>>
        return parseQuery(session.uri)
    }

    /**
     * A single handshake header value, or `null`. Header lookup is case-insensitive, as
     * `HttpHeaders` is.
     *
     * Note that a browser's `WebSocket` API cannot set custom headers; anything read here comes from a
     * non-browser client, a proxy, or `Sec-WebSocket-Protocol` smuggling.
     */
    fun header(session: WebSocketSession, name: String): String? = session.handshakeHeaders.getFirst(name)

    /**
     * Parses the **raw** query of [uri] into decoded name → values.
     *
     * Uses [URI.getRawQuery] rather than [URI.getQuery] on purpose: the latter is already decoded, so a
     * value legitimately containing `&` or `=` (a base64 token with padding, a signed URL) would be
     * split at the wrong place. Splitting first and decoding after is the only order that survives it.
     *
     * A parameter with no `=` maps to an empty string, matching how servlet containers treat `?flag`.
     */
    fun parseQuery(uri: URI?): Map<String, List<String>> {
        val raw = uri?.rawQuery
        if (raw.isNullOrEmpty()) return emptyMap()
        return raw.split('&')
            .filter { it.isNotEmpty() }
            .map { pair ->
                val separator = pair.indexOf('=')
                val name = if (separator < 0) pair else pair.substring(0, separator)
                val value = if (separator < 0) "" else pair.substring(separator + 1)
                decode(name) to decode(value)
            }
            .groupBy({ it.first }, { it.second })
    }

    /** Percent-decoding that treats a malformed escape as literal text rather than failing the handshake. */
    private fun decode(value: String): String =
        runCatching { URLDecoder.decode(value, StandardCharsets.UTF_8) }.getOrDefault(value)
}
