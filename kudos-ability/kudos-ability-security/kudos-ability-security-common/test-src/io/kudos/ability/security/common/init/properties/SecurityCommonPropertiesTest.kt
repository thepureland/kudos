package io.kudos.ability.security.common.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotSame

/**
 * Unit tests for [SecurityCommonProperties] covering default values and mutator behaviour of the
 * POJO itself (Spring binding is exercised separately in SecurityCommonAutoConfigurationTest).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class SecurityCommonPropertiesTest {

    @Test
    fun defaults_totpWindowSizeIsOne() {
        val props = SecurityCommonProperties()
        assertEquals(1, props.totp.windowSize, "default drift tolerance should be ±1 step (±30s)")
    }

    @Test
    fun totp_isMutable_andEachInstanceGetsItsOwnTotp() {
        val a = SecurityCommonProperties()
        val b = SecurityCommonProperties()
        assertNotSame(a.totp, b.totp, "each properties instance should own its Totp object")

        val replacement = SecurityCommonProperties.Totp().apply { windowSize = 17 }
        a.totp = replacement
        assertEquals(17, a.totp.windowSize)
        // b stays untouched at default
        assertEquals(1, b.totp.windowSize)
    }

    @Test
    fun totp_windowSize_acceptsBoundaryValues() {
        val totp = SecurityCommonProperties.Totp()
        totp.windowSize = 0
        assertEquals(0, totp.windowSize)
        totp.windowSize = 17
        assertEquals(17, totp.windowSize)
    }
}
