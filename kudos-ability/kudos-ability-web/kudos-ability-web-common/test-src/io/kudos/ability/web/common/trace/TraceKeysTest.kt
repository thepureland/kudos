package io.kudos.ability.web.common.trace

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Unit tests for [TraceKeys].
 *
 * This is the one place the trace-key policy is written down, so the rules are pinned here rather than
 * separately in each runtime's tests — which is the point of lifting it out of them.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class TraceKeysTest {

    private val uuidPattern =
        Regex("^[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}$")

    @Test
    fun isAcceptable_acceptsWellFormedKeys() {
        assertTrue(TraceKeys.isAcceptable("abc-123_def.456"))
        assertTrue(TraceKeys.isAcceptable("0123456789abcdef"))
        assertTrue(TraceKeys.isAcceptable("a"))
    }

    /**
     * The rejected shapes are the attack surface, not merely malformed input: CR/LF forges log entries and
     * splits response headers, and an unbounded length is a lever against a trace-id-indexed backend.
     */
    @Test
    fun isAcceptable_rejectsUnsafeKeys() {
        assertFalse(TraceKeys.isAcceptable(null), "absent header")
        assertFalse(TraceKeys.isAcceptable(""), "empty")
        assertFalse(TraceKeys.isAcceptable("   "), "blank")
        assertFalse(TraceKeys.isAcceptable("trace\r\nX-Injected: evil"), "CRLF header splitting")
        assertFalse(TraceKeys.isAcceptable("trace\nforged log line"), "LF log forging")
        assertFalse(TraceKeys.isAcceptable("trace with spaces"), "spaces")
        assertFalse(TraceKeys.isAcceptable("trace;drop"), "punctuation outside the allowed set")
        assertFalse(TraceKeys.isAcceptable("a".repeat(TraceKeys.DEFAULT_MAX_LENGTH + 1)), "over the length bound")
    }

    @Test
    fun isAcceptable_honoursTheLengthBoundExactly() {
        assertTrue(TraceKeys.isAcceptable("a".repeat(8), maxLength = 8))
        assertFalse(TraceKeys.isAcceptable("a".repeat(9), maxLength = 8))
    }

    @Test
    fun resolve_returnsTheFirstAcceptableHeader() {
        val headers = mapOf("X-Trace-Id" to "trace-public", "_UUID" to "trace-internal")

        assertEquals("trace-public", TraceKeys.resolve(listOf("X-Trace-Id", "_UUID")) { headers[it] })
        assertEquals("trace-internal", TraceKeys.resolve(listOf("_UUID", "X-Trace-Id")) { headers[it] })
    }

    /** One bad header must not discard a good value in the next; a caller controls all of them. */
    @Test
    fun resolve_skipsUnacceptableValuesRatherThanGivingUp() {
        val headers = mapOf("X-Trace-Id" to "bad value", "_UUID" to "trace-internal")

        assertEquals("trace-internal", TraceKeys.resolve(listOf("X-Trace-Id", "_UUID")) { headers[it] })
    }

    @Test
    fun resolve_generatesUuidWhenNothingUsableWasSupplied() {
        assertTrue(uuidPattern.matches(TraceKeys.resolve(listOf("X-Trace-Id")) { null }))
        assertTrue(uuidPattern.matches(TraceKeys.resolve(emptyList()) { "ignored" }))
        assertTrue(uuidPattern.matches(TraceKeys.resolve(listOf("X-Trace-Id")) { "bad value" }))
    }

    /** A blank header name is configuration meaning "not in use", not a header to look up. */
    @Test
    fun resolve_ignoresBlankHeaderNames() {
        val headers = mapOf("" to "should-never-be-read", "_UUID" to "trace-internal")

        assertEquals("trace-internal", TraceKeys.resolve(listOf("", "_UUID")) { headers[it] })
    }

}
