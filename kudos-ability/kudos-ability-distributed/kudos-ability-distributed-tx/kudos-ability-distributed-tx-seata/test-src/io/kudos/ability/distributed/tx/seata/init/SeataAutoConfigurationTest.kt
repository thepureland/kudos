package io.kudos.ability.distributed.tx.seata.init

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

/**
 * Tests for [SeataAutoConfiguration] bean factory methods and component metadata.
 *
 * Coverage:
 * - dataSourceProxy() returns a SeataDataSourceProxy instance (the @Primary override of IDataSourceProxy)
 * - each invocation creates a fresh instance (singleton-ness is the container's job, not the factory's)
 * - getComponentName() returns the expected component identifier
 *
 * @author K
 * @since 1.0.0
 */
internal class SeataAutoConfigurationTest {

    @Test
    fun dataSourceProxy_returnsSeataDataSourceProxy() {
        val configuration = SeataAutoConfiguration()

        val proxy = configuration.dataSourceProxy()

        assertIs<SeataDataSourceProxy>(proxy)
    }

    @Test
    fun dataSourceProxy_eachCallCreatesNewInstance() {
        val configuration = SeataAutoConfiguration()

        val first = configuration.dataSourceProxy()
        val second = configuration.dataSourceProxy()

        assertIs<SeataDataSourceProxy>(first)
        assertIs<SeataDataSourceProxy>(second)
        kotlin.test.assertNotSame(first, second)
    }

    @Test
    fun getComponentName_returnsModuleComponentName() {
        assertEquals("kudos-ability-distributed-tx-seata", SeataAutoConfiguration().getComponentName())
    }
}
