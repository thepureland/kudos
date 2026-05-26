package io.kudos.ability.ui.javafx.controls.wizard

import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Headless unit tests for [LinearWizardFlow].
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class LinearWizardFlowTest {

    @Test
    fun nullPagesAreTreatedAsEmptyFlow() {
        val flow = LinearWizardFlow(null)

        assertFalse(flow.canAdvance(null))
    }

    @Test
    fun emptyPagesCanNotAdvance() {
        val flow = LinearWizardFlow(emptyList())

        assertFalse(flow.canAdvance(null))
    }

}
