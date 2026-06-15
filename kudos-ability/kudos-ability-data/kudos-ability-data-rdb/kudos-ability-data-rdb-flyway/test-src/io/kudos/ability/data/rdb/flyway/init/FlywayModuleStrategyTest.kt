package io.kudos.ability.data.rdb.flyway.init

import org.flywaydb.core.Flyway
import org.mockito.Mockito
import kotlin.test.Test

/**
 * Unit test for [FlywayModuleStrategy]. Covers:
 * - migrate() is a strict no-op: it must not invoke anything on the passed-in Flyway instance
 *   (otherwise Spring Boot's default initializer would migrate the bare-bones Flyway bean).
 *
 * @author K
 * @since 1.0.0
 */
internal class FlywayModuleStrategyTest {

    /** The no-op strategy must not touch the Flyway instance at all. */
    @Test
    fun migrateIsNoOp() {
        val flyway = Mockito.mock(Flyway::class.java)
        FlywayModuleStrategy().migrate(flyway)
        Mockito.verifyNoInteractions(flyway)
    }
}
