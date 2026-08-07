package io.kudos.ability.security.enforcement.init

import io.kudos.ability.security.enforcement.init.properties.EnforcementProperties
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.core.env.Environment
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * @author K
 * @author AI: Codex
 */
internal class EnforcementProductionSafetyTest {

    private val environment = mock(Environment::class.java).also {
        whenCalled(it.activeProfiles).thenReturn(arrayOf("prod"))
    }

    @Test
    fun productionRefusesDisabledOrShadowEnforcement() {
        val configuration = EnforcementAutoConfiguration()
        assertFailsWith<IllegalStateException> {
            configuration.enforcementProductionSafetyCheck(EnforcementProperties(), environment)
                .afterSingletonsInstantiated()
        }
        assertFailsWith<IllegalStateException> {
            configuration.enforcementProductionSafetyCheck(
                EnforcementProperties().apply { enabled = true },
                environment,
            ).afterSingletonsInstantiated()
        }
    }

    @Test
    fun productionAcceptsActiveNonShadowEnforcement() {
        EnforcementAutoConfiguration().enforcementProductionSafetyCheck(
            EnforcementProperties().apply {
                enabled = true
                shadowMode = false
            },
            environment,
        ).afterSingletonsInstantiated()
    }
}
