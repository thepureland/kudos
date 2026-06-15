package io.kudos.base.error

import io.kudos.base.enums.ienums.IErrorCodeEnum
import kotlin.test.*

/**
 * Additional branch coverage for [CustomRuntimeException], complementing CustomRuntimeExceptionTest.
 *
 * Covers the protected helpers reachable only from a subclass: the single-arg
 * resolveException(errorCode) (compact stack), resolveException(message, args),
 * resolveCauseException with shouldLog=false, and the printAllStackTrace=true full-stack path.
 *
 * @author K
 * @since 1.0.0
 */
internal class CustomRuntimeExceptionBranchesTest {

    private data class Ec(
        override val code: String,
        override val defaultDisplayText: String,
        override val i18nKeyPrefix: String = ""
    ) : IErrorCodeEnum

    /** A subclass that exposes the protected resolve* helpers for direct testing. */
    private class Probe : CustomRuntimeException {
        constructor() : super()

        fun useErrorCodeOnly(ec: IErrorCodeEnum) = resolveException(ec)
        fun useErrorCodeWithStack(ec: IErrorCodeEnum, all: Boolean, vararg args: Any?) =
            resolveException(ec, all, *args)
        fun useMessage(msg: String, vararg args: Any?) = resolveException(msg, *args)
        fun useCauseNoLog(cause: Throwable, msg: String, vararg args: Any?) =
            resolveCauseException(cause, false, msg, *args)
        fun useCauseLog(cause: Throwable, msg: String, vararg args: Any?) =
            resolveCauseException(cause, msg, *args)
    }

    @Test
    fun resolveException_errorCodeOnly_compactStackAndMessage() {
        val p = Probe()
        p.useErrorCodeOnly(Ec("E", "code text"))
        assertEquals("code text", p.message)
        assertTrue(p.stackTrace.size <= 20, "compact mode keeps at most 20 frames")
    }

    @Test
    fun resolveException_errorCode_fullStack() {
        val p = Probe()
        p.useErrorCodeWithStack(Ec("E", "full {0}"), true, "stack")
        assertEquals("full stack", p.message)
        assertTrue(p.stackTrace.isNotEmpty())
    }

    @Test
    fun resolveException_message_formatsAndBlankBranch() {
        val p1 = Probe()
        p1.useMessage("hi {0}", "there")
        assertEquals("hi there", p1.message)

        val p2 = Probe()
        p2.useMessage("   ") // blank -> stored literally, no MessageFormat
        assertEquals("   ", p2.message)
    }

    @Test
    fun resolveCauseException_noLog_doesNotThrow() {
        val p = Probe()
        val cause = RuntimeException("c")
        p.useCauseNoLog(cause, "wrap {0}", "v")
        assertEquals("wrap v", p.message)
    }

    @Test
    fun resolveCauseException_withLog() {
        val p = Probe()
        val cause = RuntimeException("c")
        p.useCauseLog(cause, "logged {0}", "v2")
        assertEquals("logged v2", p.message)
    }

    @Test
    fun constructor_causeMessageLog_true() {
        // exercises CustomRuntimeException(cause, message, log=true, args)
        val cause = RuntimeException("inner")
        val ex = CustomRuntimeException(cause, "outer {0}", true, "p")
        assertEquals("outer p", ex.message)
        // note: cause is logged but not stored as getCause for this overload
    }
}
