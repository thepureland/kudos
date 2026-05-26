package io.kudos.ability.distributed.tx.seata.feign

import org.springframework.core.Ordered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for filter registration in [SeataFeignXidAutoConfiguration].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class SeataFeignXidAutoConfigurationTest {

    @Test
    fun seataXidServletFilterRegistration_usesHighestPrecedence() {
        val registration = SeataFeignXidAutoConfiguration().seataXidServletFilterRegistration()

        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.order)
        assertIs<SeataXidServletFilter>(registration.filter)
    }
}
