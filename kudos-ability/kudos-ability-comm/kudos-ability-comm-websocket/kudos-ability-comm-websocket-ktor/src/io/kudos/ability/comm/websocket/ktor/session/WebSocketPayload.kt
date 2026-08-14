package io.kudos.ability.comm.websocket.ktor.session

/**
 * What a WebSocket session can be asked to deliver: one text message or one binary message.
 *
 * Exists so that fan-out components ([io.kudos.ability.comm.websocket.ktor.broadcast.IWebSocketBroadcaster]
 * and friends) can stay payload-agnostic instead of declaring a parallel `xxxBytes` method for every
 * targeting mode. Business call sites keep using the `String` / `ByteArray` conveniences and rarely
 * name this type directly.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
sealed interface WebSocketPayload {

    /** A UTF-8 text message, delivered as one (possibly fragmented on the wire) text frame. */
    data class Text(val text: String) : WebSocketPayload

    /**
     * A binary message.
     *
     * Not a `data class`: the generated `equals` / `hashCode` would compare [bytes] by reference,
     * which silently breaks any assertion or de-duplication that treats two identical payloads as
     * equal. Structural comparison is implemented by hand instead.
     */
    class Binary(val bytes: ByteArray) : WebSocketPayload {

        override fun equals(other: Any?): Boolean =
            this === other || (other is Binary && bytes.contentEquals(other.bytes))

        override fun hashCode(): Int = bytes.contentHashCode()

        /** Deliberately prints the size rather than the content — payloads can be megabytes. */
        override fun toString(): String = "Binary(${bytes.size} bytes)"
    }
}
