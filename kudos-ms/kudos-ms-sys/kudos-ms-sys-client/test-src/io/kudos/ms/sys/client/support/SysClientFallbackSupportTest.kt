package io.kudos.ms.sys.client.support

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import kotlin.test.Test
import kotlin.test.assertIs

/**
 * Unit tests for [SysClientFallbackSupport].
 *
 * Coverage:
 *  - The namespace-alias base class can be subclassed, passes the component name through to
 *    [AbstractFeignFallbackSupport], and the inherited warnRead / errorWrite helpers never
 *    throw (a fallback must never throw) — including empty and Unicode component names,
 *    no-arg, multi-arg and null-arg calls.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysClientFallbackSupportTest {

    /** Concrete subclass exposing the inherited protected logging helpers. */
    private class TestSupport(componentName: String) : SysClientFallbackSupport(componentName) {
        fun callWarnRead(method: String, vararg args: Any?) = warnRead(method, *args)
        fun callErrorWrite(method: String, vararg args: Any?) = errorWrite(method, *args)
    }

    @Test
    fun subclass_isAbstractFeignFallbackSupport() {
        assertIs<AbstractFeignFallbackSupport>(TestSupport("TestSupport"))
    }

    @Test
    fun inheritedHelpers_neverThrow() {
        val support = TestSupport("TestSupport")
        support.callWarnRead("read")
        support.callWarnRead("read", "a", 1, null)
        support.callErrorWrite("write")
        support.callErrorWrite("write", emptyList<String>(), mapOf("k" to "v"))
    }

    @Test
    fun unusualComponentNames_doNotBreakLogging() {
        TestSupport("").callWarnRead("m")
        TestSupport("回退組件-中文").callErrorWrite("m", "參數")
    }
}
