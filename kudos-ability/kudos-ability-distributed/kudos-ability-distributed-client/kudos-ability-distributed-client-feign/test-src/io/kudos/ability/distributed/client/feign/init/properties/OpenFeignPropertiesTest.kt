package io.kudos.ability.distributed.client.feign.init.properties

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

/**
 * Unit tests for [OpenFeignProperties].
 *
 * Coverage:
 *  - default value of contextSignatureSecret is null (signing disabled).
 *  - setter round-trips normal, empty, blank and unicode values (semantic interpretation of
 *    blank-as-disabled happens in the interceptor, not here).
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
internal class OpenFeignPropertiesTest {

    @Test
    fun contextSignatureSecret_defaultsToNull() {
        assertNull(OpenFeignProperties().contextSignatureSecret)
    }

    @Test
    fun contextSignatureSecret_roundTripsValues() {
        val properties = OpenFeignProperties()

        properties.contextSignatureSecret = "secret"
        assertEquals("secret", properties.contextSignatureSecret)

        properties.contextSignatureSecret = ""
        assertEquals("", properties.contextSignatureSecret)

        properties.contextSignatureSecret = "   "
        assertEquals("   ", properties.contextSignatureSecret)

        properties.contextSignatureSecret = "密鑰-ünïcødé"
        assertEquals("密鑰-ünïcødé", properties.contextSignatureSecret)

        properties.contextSignatureSecret = null
        assertNull(properties.contextSignatureSecret)
    }
}
