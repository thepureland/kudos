package io.kudos.ms.msg.api.internal.init

import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * test for MsgApiProviderApplication (pure instantiation, no Spring container).
 *
 * Note: [main] is intentionally not exercised here -- it boots a full SpringApplication that requires
 * a database, Nacos discovery/config and other infrastructure, so it belongs to the serial integration
 * stage rather than a pure unit test.
 *
 * @author K
 * @since 1.0.0
 */
internal class MsgApiProviderApplicationTest {

    @Test
    fun canBeInstantiated() {
        assertNotNull(MsgApiProviderApplication())
    }
}
