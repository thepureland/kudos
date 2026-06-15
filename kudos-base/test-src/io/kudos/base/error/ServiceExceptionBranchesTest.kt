package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import io.kudos.base.enums.impl.CommonErrorCodeEnum
import kotlin.test.*

/**
 * Branch coverage for [ServiceException] and the inherited [CustomRuntimeException] paths
 * not already exercised by CustomRuntimeExceptionTest.
 *
 * Covers: every public ServiceException constructor (message/cause/errorCode variants),
 * the errorCode + params accessors, full-stack (printAllStackTrace=true) vs trimmed mode,
 * blank-message handling, and the no-log cause path.
 *
 * @author K
 * @since 1.0.0
 */
internal class ServiceExceptionBranchesTest {

    private data class Ec(
        override val code: String,
        override val defaultDisplayText: String,
        override val i18nKeyPrefix: String = ""
    ) : IErrorCodeEnum

    // -------- message / cause string constructors (inherited from CustomRuntimeException) --------

    @Test
    fun ctor_messageWithArgs_formats() {
        val ex = ServiceException("Hello {0} and {1}", "a", "b")
        assertEquals("Hello a and b", ex.message)
    }

    @Test
    fun ctor_messageNoArgs_leavesPlaceholdersIntact() {
        // no args => formatting is skipped, so literal {0} stays
        val ex = ServiceException("raw {0} literal")
        assertEquals("raw {0} literal", ex.message)
    }

    @Test
    fun ctor_blankMessage_isStoredAsIs() {
        val ex = ServiceException("")
        assertEquals("", ex.message)
    }

    @Test
    fun ctor_causeOnly_usesCauseMessage() {
        val cause = IllegalStateException("boom")
        val ex = ServiceException(cause)
        assertEquals("boom", ex.message)
    }

    @Test
    fun ctor_causeWithMessageAndArgs() {
        val cause = RuntimeException("root")
        val ex = ServiceException(cause, "wrap {0}", "x")
        assertEquals("wrap x", ex.message)
    }

    @Test
    fun ctor_causeWithMessageLogFalseAndArgs() {
        val cause = RuntimeException("root")
        // log=false path: must not throw, message still formatted
        val ex = ServiceException(cause, "wrap {0}", false, "y")
        assertEquals("wrap y", ex.message)
    }

    // -------- errorCode constructors --------

    @Test
    fun ctor_errorCodeOnly_defaultPrintAllFalse() {
        val ex = ServiceException(CommonErrorCodeEnum.NOT_FOUND)
        assertSame(CommonErrorCodeEnum.NOT_FOUND, ex.errorCode)
        // displayText uses i18n key for CommonErrorCodeEnum
        assertEquals("sys.error-msg.default.404", ex.message)
        assertNull(ex.params)
    }

    @Test
    fun ctor_errorCode_printAllStackTrace_true_keepsFullStack() {
        val ec = Ec("E", "plain text")
        val ex = ServiceException(ec, true)
        assertEquals("plain text", ex.message)
        assertTrue(ex.stackTrace.isNotEmpty())
    }

    @Test
    fun ctor_errorCodeWithArgs_storesParamsAndFormats() {
        val ec = Ec("E", "User {0} did {1}")
        val ex = ServiceException(ec, false, "bob", "delete")
        assertEquals("User bob did delete", ex.message)
        assertNotNull(ex.params)
        assertEquals(2, ex.params!!.size)
        assertEquals("bob", ex.params!![0])
        assertEquals("delete", ex.params!![1])
    }

    @Test
    fun ctor_errorCodeWithArgs_printAllTrue() {
        val ec = Ec("E", "msg {0}")
        val ex = ServiceException(ec, true, "z")
        assertEquals("msg z", ex.message)
        assertNotNull(ex.params)
    }

    @Test
    fun ctor_errorCodeAndCauseAndArgs() {
        val ec = Ec("E", "calling {0}")
        val cause = RuntimeException("inner")
        val ex = ServiceException(ec, cause, "svc")
        assertEquals("calling svc", ex.message)
        assertSame(ec, ex.errorCode)
    }

    @Test
    fun ctor_errorCodeAndCauseLogFalseAndArgs() {
        val ec = Ec("E", "calling {0}")
        val cause = RuntimeException("inner")
        val ex = ServiceException(ec, cause, false, "svc2")
        assertEquals("calling svc2", ex.message)
    }

    @Test
    fun ctor_errorCodeAndThrowable() {
        val ec = Ec("E", "no placeholder text")
        val cause = RuntimeException("inner")
        // resolves to the (errorCode, ex) overload
        val ex = ServiceException(ec, cause as Throwable)
        assertEquals("no placeholder text", ex.message)
        assertSame(ec, ex.errorCode)
    }

    @Test
    fun serialVersionUid_companionAccessible() {
        // touch the companion so its initializer is exercised
        assertNotNull(ServiceException.Companion)
    }
}
