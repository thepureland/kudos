package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * CustomRuntimeException test cases
 *
 * @author AI: ChatGPT
 * @since 1.0.0
 */
internal class CustomRuntimeExceptionTest {

    @Test
    fun testConstructorWithCauseHavingNullMessage() {
        val cause = NullPointerException()
        val exception = CustomRuntimeException(cause)

        assertNotNull(exception.message)
        assertEquals(NullPointerException::class.java.name, exception.message)
    }

    @Test
    fun testErrorCodeMessageFormattingWithArgs() {
        val errorCode = TestErrorCode("E001", "User {0} operation failed ({1})")
        val exception = ServiceException(errorCode, false, "alice", 42)

        assertEquals("User alice operation failed (42)", exception.message)
    }

    @Test
    fun testErrorCodeMessageFormattingWithCauseAndArgs() {
        val errorCode = TestErrorCode("E002", "Error when calling {0}")
        val cause = RuntimeException("inner")
        val exception = ServiceException(errorCode, cause, "minio")

        assertEquals("Error when calling minio", exception.message)
    }

    @Test
    fun testErrorCodeMessageFormattingWithCauseLogAndArgs() {
        val errorCode = TestErrorCode("E003", "Operation {0} failed, reason code {1}")
        val cause = RuntimeException("inner")
        val exception = ServiceException(errorCode, cause, false, "delete", 7)

        assertEquals("Operation delete failed, reason code 7", exception.message)
    }

    /**
     * In trimmed mode, business-caller frames should be preserved and the total count should not exceed 20.
     * Key check: the old fixed-5-frame implementation truncated deep business calls; the new 20-frame implementation should retain them.
     */
    @Test
    fun testTruncatedStackPreservesBusinessCaller() {
        val errorCode = TestErrorCode("E100", "test")
        val exception = createDeeplyNested(errorCode, depth = 8)
        val trace = exception.stackTrace

        assertTrue(trace.isNotEmpty(), "Trimmed mode should not clear the stack")
        assertTrue(trace.size <= 20, "Trimmed mode keeps at most 20 frames, actual: ${trace.size}")
        // The 8-level-deep helper chain and the outermost test method should be visible in the trimmed stack
        val classNames = trace.map { it.className }
        assertTrue(
            classNames.contains(CustomRuntimeExceptionTest::class.java.name),
            "Trimmed stack should contain the business caller, actual stack: $classNames"
        )
    }

    private fun createDeeplyNested(errorCode: IErrorCodeEnum, depth: Int): ServiceException {
        return if (depth <= 0) ServiceException(errorCode, false)
        else createDeeplyNested(errorCode, depth - 1)
    }

    private data class TestErrorCode(
        override val code: String,
        override val defaultDisplayText: String,
        override val i18nKeyPrefix: String = ""
    ) : IErrorCodeEnum
}
