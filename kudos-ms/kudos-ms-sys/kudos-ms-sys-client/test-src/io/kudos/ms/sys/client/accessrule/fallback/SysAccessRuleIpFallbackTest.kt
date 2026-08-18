package io.kudos.ms.sys.client.accessrule.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.sys.client.accessrule.proxy.ISysAccessRuleIpProxy
import java.math.BigDecimal
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Unit tests for [SysAccessRuleIpFallback].
 *
 * Coverage:
 *  - The fallback is a valid [ISysAccessRuleIpProxy] / [AbstractFeignFallbackSupport]
 *    implementation, keeping it usable as a `@FeignClient(fallback=...)` target.
 *  - Read methods (getIpsByRuleId / getIpsBySystemAndTenant) return empty list safe defaults,
 *    including the nullable tenantId parameter (null and non-null).
 *  - checkIpAccess returns false (security-side deny default) for zero, negative, IPv4-range
 *    and IPv6-range BigDecimal values, with both null and non-null tenantId.
 *  - deleteByRuleId (write) returns the failure default 0.
 *
 * @author K
 * @since 1.0.0
 */
internal class SysAccessRuleIpFallbackTest {

    private val fallback = SysAccessRuleIpFallback()

    @Test
    fun instantiation_yieldsValidProxyFallback() {
        assertIs<ISysAccessRuleIpProxy>(fallback)
        assertIs<AbstractFeignFallbackSupport>(fallback)
    }

    @Test
    fun getIpsByRuleId_returnsEmptyList() {
        assertTrue(fallback.getIpsByRuleId("rule-1").isEmpty())
        assertTrue(fallback.getIpsByRuleId("").isEmpty())
    }

    @Test
    fun getIpsBySystemAndTenant_returnsEmptyList_withAndWithoutTenant() {
        assertTrue(fallback.getIpsBySystemAndTenant("sys-1", "tenant-1").isEmpty())
        assertTrue(fallback.getIpsBySystemAndTenant("sys-1", null).isEmpty())
        assertTrue(fallback.getIpsBySystemAndTenant("", "").isEmpty())
    }

    @Test
    fun checkIpAccess_returnsFalse_denyByDefault() {
        assertFalse(fallback.checkIpAccess(BigDecimal.ZERO, "sys-1", null))
        assertFalse(fallback.checkIpAccess(BigDecimal("3232235777"), "sys-1", "tenant-1")) // 192.168.1.1
        assertFalse(fallback.checkIpAccess(BigDecimal("-1"), "sys-1", null))
        // Max IPv6 value (2^128 - 1)
        assertFalse(
            fallback.checkIpAccess(
                BigDecimal("340282366920938463463374607431768211455"), "系統-ünï", null
            )
        )
    }

    @Test
    fun deleteByRuleId_returnsZero() {
        assertEquals(0, fallback.deleteByRuleId("rule-1"))
        assertEquals(0, fallback.deleteByRuleId(""))
    }
}
