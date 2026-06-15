package io.kudos.ability.distributed.client.feign.fallback

import java.net.ConnectException
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for [AbstractFeignFallbackSupport].
 *
 * Coverage:
 *  - [AbstractFeignFallbackSupport.describeStatus] all branches: null cause -> "unknown";
 *    FeignException status 0 -> "unreachable"; 4xx boundaries (400/404/499) -> "client-error-x";
 *    5xx boundaries (500/503/599) -> "server-error-x"; out-of-range status (3xx / 600) -> "status-x";
 *    non-Feign exception -> "exception-SimpleName".
 *  - warnRead / errorWrite: both the cause-less and cause-taking overloads run without throwing
 *    (a fallback must never throw), including null cause, no args, multiple args and null args.
 *  - Unicode and empty component names do not break logging.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class AbstractFeignFallbackSupportTest {

    /** Concrete subclass exposing the protected logging helpers for direct testing. */
    private class TestFallback(componentName: String = "TestFallback") :
        AbstractFeignFallbackSupport(componentName) {
        fun callWarnRead(method: String, vararg args: Any?) = warnRead(method, *args)
        fun callWarnReadWithCause(method: String, cause: Throwable?, vararg args: Any?) =
            warnRead(method, cause, *args)
        fun callErrorWrite(method: String, vararg args: Any?) = errorWrite(method, *args)
        fun callErrorWriteWithCause(method: String, cause: Throwable?, vararg args: Any?) =
            errorWrite(method, cause, *args)
    }

    private val fallback = TestFallback()

    @Test
    fun describeStatus_nullCause_isUnknown() {
        assertEquals("unknown", fallback.describeStatus(null))
    }

    @Test
    fun describeStatus_feignStatusZero_isUnreachable() {
        assertEquals("unreachable", fallback.describeStatus(TestFeignException(0)))
    }

    @Test
    fun describeStatus_clientErrorBoundaries() {
        assertEquals("client-error-400", fallback.describeStatus(TestFeignException(400)))
        assertEquals("client-error-404", fallback.describeStatus(TestFeignException(404)))
        assertEquals("client-error-499", fallback.describeStatus(TestFeignException(499)))
    }

    @Test
    fun describeStatus_serverErrorBoundaries() {
        assertEquals("server-error-500", fallback.describeStatus(TestFeignException(500)))
        assertEquals("server-error-503", fallback.describeStatus(TestFeignException(503)))
        assertEquals("server-error-599", fallback.describeStatus(TestFeignException(599)))
    }

    @Test
    fun describeStatus_otherStatuses_fallThroughToStatusPrefix() {
        assertEquals("status-302", fallback.describeStatus(TestFeignException(302)))
        assertEquals("status-600", fallback.describeStatus(TestFeignException(600)))
        assertEquals("status--1", fallback.describeStatus(TestFeignException(-1)))
    }

    @Test
    fun describeStatus_nonFeignException_usesSimpleClassName() {
        assertEquals(
            "exception-IllegalStateException",
            fallback.describeStatus(IllegalStateException("boom"))
        )
        assertEquals(
            "exception-ConnectException",
            fallback.describeStatus(ConnectException("refused"))
        )
    }

    @Test
    fun warnRead_neverThrows_withAndWithoutArgs() {
        fallback.callWarnRead("getUser")
        fallback.callWarnRead("getUser", 1L, "name", null)
        fallback.callWarnRead("getUser", "unicode-名字-ünïcødé")
    }

    @Test
    fun warnReadWithCause_neverThrows_forAllCauseKinds() {
        fallback.callWarnReadWithCause("getUser", null)
        fallback.callWarnReadWithCause("getUser", TestFeignException(404), 1)
        fallback.callWarnReadWithCause("getUser", IllegalArgumentException("bad"), 1, null)
    }

    @Test
    fun errorWrite_neverThrows_withAndWithoutArgs() {
        fallback.callErrorWrite("saveUser")
        fallback.callErrorWrite("saveUser", mapOf("k" to "v"), emptyList<String>())
    }

    @Test
    fun errorWriteWithCause_neverThrows_forAllCauseKinds() {
        fallback.callErrorWriteWithCause("saveUser", null)
        fallback.callErrorWriteWithCause("saveUser", TestFeignException(500), "arg")
        fallback.callErrorWriteWithCause("saveUser", RuntimeException("rt"))
    }

    @Test
    fun unusualComponentNames_doNotBreakLogging() {
        TestFallback("").callWarnRead("m")
        TestFallback("回退組件-中文").callErrorWrite("m", "參數")
    }
}
