package io.kudos.ability.comm.websocket.common.session

/**
 * Engine-neutral WebSocket close reason: an RFC 6455 status code plus an optional human-readable
 * message.
 *
 * This type exists so the abstractions in this module — [KudosWebSocketSessionRef],
 * [io.kudos.ability.comm.websocket.common.broadcast.IWebSocketBroadcaster],
 * [io.kudos.ability.comm.websocket.common.connect.IWebSocketConnectInterceptor] — can express
 * "close the connection, and why" without naming any engine's own close type. Before it existed,
 * `close()` took Ktor's `io.ktor.websocket.CloseReason`, which quietly made every "engine-agnostic"
 * component depend on `ktor-websockets`; a Spring MVC application could not use the registry or the
 * broadcaster without pulling Ktor onto its classpath, which is the opposite of what the abstraction
 * was for.
 *
 * Each engine module maps this to its own type at the single point where it touches the wire —
 * `io.ktor.websocket.CloseReason` in the Ktor module, `org.springframework.web.socket.CloseStatus`
 * in the Spring module.
 *
 * [code] is a `Short` because that is what the protocol puts on the wire (a two-byte big-endian
 * integer in the Close frame's payload); widening it to `Int` would invite values that cannot be
 * sent.
 *
 * @property code RFC 6455 status code; see [Codes] for the standard ones
 * @property message optional reason phrase, capped at 123 UTF-8 bytes on the wire — see [Codes]
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
data class WebSocketCloseReason(val code: Short, val message: String = "") {

    /** Builds a reason from a well-known [Codes] entry. */
    constructor(code: Codes, message: String = "") : this(code.code, message)

    /** The [Codes] entry matching [code], or `null` for a non-standard / application-defined code. */
    val knownCode: Codes?
        get() = Codes.byCode(code)

    /**
     * Standard close codes from RFC 6455 §7.4.1.
     *
     * The reserved pseudo-codes are included because a *received* close can carry them, but note
     * that [NO_STATUS_CODE] (1005) and [CLOSED_ABNORMALLY] (1006) must never be **sent** — they
     * signal "no Close frame arrived at all", so putting them in one is a protocol violation that
     * engines reject.
     */
    enum class Codes(val code: Short) {
        /** 1000 — the purpose of the connection has been fulfilled. */
        NORMAL(1000),

        /** 1001 — endpoint is going away (server shutting down, browser navigating away). */
        GOING_AWAY(1001),

        /** 1002 — a protocol error was detected. */
        PROTOCOL_ERROR(1002),

        /** 1003 — the peer sent a data type this endpoint cannot accept. */
        CANNOT_ACCEPT(1003),

        /** 1005 — reserved: no status code was present. Never send this. */
        NO_STATUS_CODE(1005),

        /** 1006 — reserved: the connection dropped without a Close frame. Never send this. */
        CLOSED_ABNORMALLY(1006),

        /** 1007 — message payload was not consistent with its type (e.g. invalid UTF-8 in a text frame). */
        NOT_CONSISTENT(1007),

        /** 1008 — a message violated policy. The catch-all when no more specific code fits. */
        VIOLATED_POLICY(1008),

        /** 1009 — the message was too large to process. */
        TOO_BIG(1009),

        /** 1010 — the client expected an extension the server did not negotiate. */
        NO_EXTENSION(1010),

        /** 1011 — an unexpected condition prevented the server from fulfilling the request. */
        INTERNAL_ERROR(1011),

        /** 1012 — the service is restarting. */
        SERVICE_RESTART(1012),

        /** 1013 — the service is overloaded; the client should retry later. */
        TRY_AGAIN_LATER(1013);

        companion object {
            private val BY_CODE: Map<Short, Codes> = entries.associateBy { it.code }

            /** @return the entry for [code], or `null` when it is not one of the standard codes */
            fun byCode(code: Short): Codes? = BY_CODE[code]
        }
    }

    companion object {

        /** The ordinary "we're done here" close, with no reason phrase. */
        val NORMAL: WebSocketCloseReason = WebSocketCloseReason(Codes.NORMAL)

        /** Sent to still-open sessions when the process is shutting down. */
        val GOING_AWAY: WebSocketCloseReason = WebSocketCloseReason(Codes.GOING_AWAY)

        /**
         * Default rejection for a connection that could not be identified or admitted.
         *
         * `VIOLATED_POLICY` rather than `INTERNAL_ERROR`: the connection was refused by a rule, and a
         * client should not retry it unchanged.
         */
        val REJECTED: WebSocketCloseReason = WebSocketCloseReason(Codes.VIOLATED_POLICY, "Connection rejected")
    }
}
