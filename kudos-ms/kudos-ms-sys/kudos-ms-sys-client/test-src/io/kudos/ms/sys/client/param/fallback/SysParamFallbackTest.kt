package io.kudos.ms.sys.client.param.fallback

import kotlin.test.Test
import kotlin.test.assertNull

/**
 * Unit tests for [SysParamFallback].
 *
 * Coverage:
 *  - getParam returns the null safe default.
 *  - Edge inputs: empty strings, Unicode, very long names.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysParamFallbackTest {

    private val fallback = SysParamFallback()

    @Test
    fun getParam_returnsNull() {
        assertNull(fallback.getParam("param-1", "atomic-1"))
        assertNull(fallback.getParam("", ""))
        assertNull(fallback.getParam("參數-ünï", "服務-ünï"))
        assertNull(fallback.getParam("p".repeat(10_000), "atomic-1"))
    }
}
