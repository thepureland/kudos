package io.kudos.base.error

import kotlin.test.*

/**
 * Tests for the simple RuntimeException subclasses:
 * [ObjectAlreadyExistsException], [ObjectNotFoundException] and [IllegalOperationException].
 *
 * Covers both constructors of each (message+cause, and the secondary Throwable-only
 * constructor that derives the message from the cause), including the null-message path.
 *
 * @author K
 * @since 1.0.0
 */
internal class SimpleExceptionsTest {

    @Test
    fun objectAlreadyExists_messageConstructor() {
        val ex = ObjectAlreadyExistsException("dup")
        assertEquals("dup", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun objectAlreadyExists_messageAndCauseConstructor() {
        val cause = IllegalStateException("root")
        val ex = ObjectAlreadyExistsException("dup", cause)
        assertEquals("dup", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun objectAlreadyExists_throwableConstructor_copiesMessage() {
        val cause = RuntimeException("inner-already")
        val ex = ObjectAlreadyExistsException(cause)
        assertEquals("inner-already", ex.message)
        assertSame(cause, ex.cause)
        assertTrue(ex is RuntimeException)
    }

    @Test
    fun objectAlreadyExists_throwableConstructor_nullMessage() {
        val cause = RuntimeException() // null message
        val ex = ObjectAlreadyExistsException(cause)
        assertNull(ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun objectNotFound_messageConstructor() {
        val ex = ObjectNotFoundException("missing")
        assertEquals("missing", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun objectNotFound_messageAndCauseConstructor() {
        val cause = IllegalArgumentException("bad")
        val ex = ObjectNotFoundException("missing", cause)
        assertEquals("missing", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun objectNotFound_throwableConstructor_copiesMessage() {
        val cause = RuntimeException("inner-missing")
        val ex = ObjectNotFoundException(cause)
        assertEquals("inner-missing", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun objectNotFound_throwableConstructor_nullMessage() {
        val cause = RuntimeException()
        val ex = ObjectNotFoundException(cause)
        assertNull(ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun illegalOperation_messageConstructor() {
        val ex = IllegalOperationException("nope")
        assertEquals("nope", ex.message)
        assertNull(ex.cause)
    }

    @Test
    fun illegalOperation_messageAndCauseConstructor() {
        val cause = IllegalStateException("state")
        val ex = IllegalOperationException("nope", cause)
        assertEquals("nope", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun illegalOperation_throwableConstructor_copiesMessage() {
        val cause = RuntimeException("inner-illegal")
        val ex = IllegalOperationException(cause)
        assertEquals("inner-illegal", ex.message)
        assertSame(cause, ex.cause)
    }

    @Test
    fun illegalOperation_throwableConstructor_nullMessage() {
        val cause = RuntimeException()
        val ex = IllegalOperationException(cause)
        assertNull(ex.message)
        assertSame(cause, ex.cause)
    }
}
