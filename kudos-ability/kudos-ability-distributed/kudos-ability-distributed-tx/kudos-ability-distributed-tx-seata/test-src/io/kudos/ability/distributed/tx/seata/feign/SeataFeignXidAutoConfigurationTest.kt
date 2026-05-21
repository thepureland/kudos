package io.kudos.ability.distributed.tx.seata.feign

import org.springframework.core.Ordered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

internal class SeataFeignXidAutoConfigurationTest {

    @Test
    fun seataXidServletFilterRegistration_usesHighestPrecedence() {
        val registration = SeataFeignXidAutoConfiguration().seataXidServletFilterRegistration()

        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.order)
        assertIs<SeataXidServletFilter>(registration.filter)
    }
}
