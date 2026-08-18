package io.kudos.ability.comm.websocket.common.session

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

/**
 * Unit tests for [WebSocketCloseReason], the engine-neutral close reason.
 *
 * Small type, but it is the seam that lets this module depend on no WebSocket engine at all, and its
 * numeric codes go on the wire — a wrong constant is a protocol error the compiler cannot catch. The
 * codes are therefore asserted against RFC 6455 §7.4.1 literally rather than against each other.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class WebSocketCloseReasonTest {

    @Test
    fun codes_matchRfc6455() {
        assertEquals(1000, WebSocketCloseReason.Codes.NORMAL.code)
        assertEquals(1001, WebSocketCloseReason.Codes.GOING_AWAY.code)
        assertEquals(1002, WebSocketCloseReason.Codes.PROTOCOL_ERROR.code)
        assertEquals(1003, WebSocketCloseReason.Codes.CANNOT_ACCEPT.code)
        assertEquals(1005, WebSocketCloseReason.Codes.NO_STATUS_CODE.code)
        assertEquals(1006, WebSocketCloseReason.Codes.CLOSED_ABNORMALLY.code)
        assertEquals(1007, WebSocketCloseReason.Codes.NOT_CONSISTENT.code)
        assertEquals(1008, WebSocketCloseReason.Codes.VIOLATED_POLICY.code)
        assertEquals(1009, WebSocketCloseReason.Codes.TOO_BIG.code)
        assertEquals(1010, WebSocketCloseReason.Codes.NO_EXTENSION.code)
        assertEquals(1011, WebSocketCloseReason.Codes.INTERNAL_ERROR.code)
        assertEquals(1012, WebSocketCloseReason.Codes.SERVICE_RESTART.code)
        assertEquals(1013, WebSocketCloseReason.Codes.TRY_AGAIN_LATER.code)
    }

    @Test
    fun codesAreUnique() {
        val codes = WebSocketCloseReason.Codes.entries.map { it.code }
        assertEquals(codes.size, codes.toSet().size, "Two enum entries sharing a code would make byCode ambiguous")
    }

    @Test
    fun byCode_resolvesEveryKnownCode() {
        WebSocketCloseReason.Codes.entries.forEach { expected ->
            assertSame(expected, WebSocketCloseReason.Codes.byCode(expected.code))
        }
    }

    /** Application-defined codes (4000-4999) are legal on the wire and must not blow up the lookup. */
    @Test
    fun byCode_returnsNullForNonStandardCodes() {
        assertNull(WebSocketCloseReason.Codes.byCode(4000))
        assertNull(WebSocketCloseReason.Codes.byCode(1004))
        assertNull(WebSocketCloseReason.Codes.byCode(0))
    }

    @Test
    fun theCodesConstructor_carriesTheNumericCode() {
        val reason = WebSocketCloseReason(WebSocketCloseReason.Codes.TOO_BIG, "message too big")

        assertEquals(1009, reason.code)
        assertEquals("message too big", reason.message)
    }

    @Test
    fun messageDefaultsToEmptyRatherThanNull() {
        assertEquals("", WebSocketCloseReason(WebSocketCloseReason.Codes.NORMAL).message)
        assertEquals("", WebSocketCloseReason(1000).message)
    }

    @Test
    fun knownCode_classifiesStandardAndApplicationCodes() {
        assertEquals(WebSocketCloseReason.Codes.VIOLATED_POLICY, WebSocketCloseReason(1008).knownCode)
        assertNull(WebSocketCloseReason(4321, "app specific").knownCode)
    }

    @Test
    fun theSharedConstants_areWhatTheirNamesSay() {
        assertEquals(1000, WebSocketCloseReason.NORMAL.code)
        assertEquals("", WebSocketCloseReason.NORMAL.message)

        assertEquals(1001, WebSocketCloseReason.GOING_AWAY.code)

        // VIOLATED_POLICY, not INTERNAL_ERROR: the connection was refused by a rule, and a client
        // should not read it as "retry the same thing later".
        assertEquals(1008, WebSocketCloseReason.REJECTED.code)
        assertEquals("Connection rejected", WebSocketCloseReason.REJECTED.message)
    }

    @Test
    fun equalityIsStructural() {
        assertEquals(WebSocketCloseReason(1000, "bye"), WebSocketCloseReason(1000, "bye"))
        assertEquals(
            WebSocketCloseReason(WebSocketCloseReason.Codes.NORMAL, "bye").hashCode(),
            WebSocketCloseReason(1000, "bye").hashCode(),
        )
        assertNotEquals(WebSocketCloseReason(1000, "bye"), WebSocketCloseReason(1001, "bye"))
        assertNotEquals(WebSocketCloseReason(1000, "bye"), WebSocketCloseReason(1000, "later"))
    }
}
