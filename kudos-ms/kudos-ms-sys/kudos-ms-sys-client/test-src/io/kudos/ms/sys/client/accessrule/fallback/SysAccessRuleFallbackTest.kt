package io.kudos.ms.sys.client.accessrule.fallback

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

/**
 * Unit tests for [SysAccessRuleFallback].
 *
 * Coverage:
 *  - getAccessRuleByTenantAndSystem returns the null safe default.
 *  - updateActive (write) returns the failure default false for both flag values.
 *  - Edge inputs: empty strings, Unicode.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleFallbackTest {

    private val fallback = SysAccessRuleFallback()

    @Test
    fun getAccessRuleByTenantAndSystem_returnsNull() {
        assertNull(fallback.getAccessRuleByTenantAndSystem("sys-1", "tenant-1"))
        assertNull(fallback.getAccessRuleByTenantAndSystem("", ""))
        assertNull(fallback.getAccessRuleByTenantAndSystem("系統-ünï", "租戶"))
    }

    @Test
    fun updateActive_returnsFalse_forBothFlagValues() {
        assertFalse(fallback.updateActive("rule-1", true))
        assertFalse(fallback.updateActive("rule-1", false))
        assertFalse(fallback.updateActive("", true))
    }
}
