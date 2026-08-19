package io.kudos.ability.distributed.tx.seata.http

import org.springframework.core.Ordered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [SeataXidPropagationAutoConfiguration].
 *
 * Coverage:
 * - seataXidServletFilterRegistration(): registers a SeataXidServletFilter on "/&#42;" with HIGHEST_PRECEDENCE
 * - seataXidRequestProcessor(): produces the outbound XID header processor
 * - getComponentName(): returns the expected component identifier
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class SeataXidPropagationAutoConfigurationTest {

    @Test
    fun seataXidServletFilterRegistration_usesHighestPrecedence() {
        val registration = SeataXidPropagationAutoConfiguration().seataXidServletFilterRegistration()

        assertEquals(Ordered.HIGHEST_PRECEDENCE, registration.order)
        assertIs<SeataXidServletFilter>(registration.filter)
        assertTrue("/*" in registration.urlPatterns)
    }

    @Test
    fun seataXidRequestProcessor_returnsProcessorInstance() {
        val processor = SeataXidPropagationAutoConfiguration().seataXidRequestProcessor()

        assertNotNull(processor)
    }

    @Test
    fun getComponentName_returnsModuleComponentName() {
        assertEquals(
            "kudos-ability-distributed-tx-seata-xid",
            SeataXidPropagationAutoConfiguration().getComponentName()
        )
    }
}
