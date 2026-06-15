package io.kudos.ability.ui.javafx.controls.wizard

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Headless unit tests for [LinearWizardFlow].
 *
 * Covers null/empty page collections, sequential advance from null and between pages,
 * canAdvance boundaries, the vararg constructor, advancing past the last page
 * (IndexOutOfBoundsException), null page entries and the defensive copy of the page collection.
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

    @Test
    fun advanceFromNullReturnsFirstPage() = runFx {
        val p1 = Wizard.WizardPane()
        val p2 = Wizard.WizardPane()
        val flow = LinearWizardFlow(p1, p2)

        assertSame(p1, flow.advance(null).get())
    }

    @Test
    fun advanceMovesSequentiallyThroughPages() = runFx {
        val p1 = Wizard.WizardPane()
        val p2 = Wizard.WizardPane()
        val p3 = Wizard.WizardPane()
        val flow = LinearWizardFlow(listOf(p1, p2, p3))

        assertSame(p2, flow.advance(p1).get())
        assertSame(p3, flow.advance(p2).get())
    }

    @Test
    fun canAdvanceIsTrueUntilLastPage() = runFx {
        val p1 = Wizard.WizardPane()
        val p2 = Wizard.WizardPane()
        val flow = LinearWizardFlow(p1, p2)

        assertTrue(flow.canAdvance(null))
        assertTrue(flow.canAdvance(p1))
        assertFalse(flow.canAdvance(p2))
    }

    @Test
    fun unknownPageIsTreatedAsBeforeFirstPage() = runFx {
        val p1 = Wizard.WizardPane()
        val stranger = Wizard.WizardPane()
        val flow = LinearWizardFlow(p1)

        // indexOf(unknown) == -1, so the flow can still "advance" to the first page
        assertTrue(flow.canAdvance(stranger))
        assertSame(p1, flow.advance(stranger).get())
    }

    @Test
    fun advancePastLastPageThrowsIndexOutOfBounds() = runFx {
        val p1 = Wizard.WizardPane()
        val flow = LinearWizardFlow(p1)

        assertFailsWith<IndexOutOfBoundsException> { flow.advance(p1) }
    }

    @Test
    fun nullPageEntryYieldsEmptyOptional() = runFx {
        val p1 = Wizard.WizardPane()
        val flow = LinearWizardFlow(listOf(p1, null))

        // a null currentPage is *found* in a list containing null, so test via a null successor
        assertFalse(flow.advance(p1).isPresent)
    }

    @Test
    fun pagesAreDefensivelyCopiedAtConstruction() = runFx {
        val p1 = Wizard.WizardPane()
        val source = mutableListOf<Wizard.WizardPane?>(p1)
        val flow = LinearWizardFlow(source)

        source.clear()

        assertTrue(flow.canAdvance(null))
        assertSame(p1, flow.advance(null).get())
    }
}
