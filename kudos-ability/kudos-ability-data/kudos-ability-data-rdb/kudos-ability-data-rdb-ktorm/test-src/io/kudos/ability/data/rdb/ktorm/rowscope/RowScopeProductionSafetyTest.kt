package io.kudos.ability.data.rdb.ktorm.rowscope

import org.mockito.Mockito.mock
import org.mockito.Mockito.`when` as whenCalled
import org.springframework.core.env.Environment
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * @author K
 * @author AI: Codex
 */
internal class RowScopeProductionSafetyTest {

    private val environment = mock(Environment::class.java).also {
        whenCalled(it.activeProfiles).thenReturn(arrayOf("production"))
    }

    @Test
    fun productionRefusesDisabledOrShadowRowScope() {
        val configuration = RowScopeAutoConfiguration()
        assertFailsWith<IllegalStateException> {
            configuration.rowScopeProductionSafetyCheck(RowScopeProperties(), environment)
                .afterSingletonsInstantiated()
        }
        assertFailsWith<IllegalStateException> {
            configuration.rowScopeProductionSafetyCheck(
                RowScopeProperties().apply { enabled = true },
                environment,
            ).afterSingletonsInstantiated()
        }
    }

    @Test
    fun productionAcceptsActiveNonShadowRowScope() {
        RowScopeAutoConfiguration().rowScopeProductionSafetyCheck(
            RowScopeProperties().apply {
                enabled = true
                shadowMode = false
            },
            environment,
        ).afterSingletonsInstantiated()
    }
}
