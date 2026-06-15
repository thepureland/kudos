package io.kudos.ability.distributed.tx.seata.feign

import org.springframework.core.Ordered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [SeataFeignXidAutoConfiguration].
 *
 * Coverage:
 * - seataXidServletFilterRegistration(): registers a SeataXidServletFilter on "/&#42;" with HIGHEST_PRECEDENCE
 * - seataFeignXidProcessor(): produces the outbound XID header processor
 * - getComponentName(): returns the expected component identifier
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
        assertTrue("/*" in registration.urlPatterns)
    }

    @Test
    fun seataFeignXidProcessor_returnsProcessorInstance() {
        val processor = SeataFeignXidAutoConfiguration().seataFeignXidProcessor()

        assertNotNull(processor)
    }

    @Test
    fun getComponentName_returnsModuleComponentName() {
        assertEquals(
            "kudos-ability-distributed-tx-seata-feign-xid",
            SeataFeignXidAutoConfiguration().getComponentName()
        )
    }
}
