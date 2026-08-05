package io.kudos.ability.ui.javafx.controls.wizard

import io.kudos.ability.ui.javafx.FxTestSupport.runFx
import javafx.application.Platform
import javafx.scene.control.Button
import javafx.scene.control.ButtonBar.ButtonData
import javafx.scene.control.ButtonType
import javafx.scene.control.CheckBox
import javafx.scene.control.Label
import javafx.scene.control.Slider
import javafx.scene.control.TextField
import javafx.scene.layout.HBox
import javafx.scene.layout.VBox
import javafx.stage.Window
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * test for Wizard
 *
 * Covers: both constructors, default flow, flowProperty/getFlow/setFlow, properties map
 * (getProperties/hasProperties/setUserData/userData), button installation on setFlow
 * (Previous/Next/Finish/Cancel and the Next-first / Finish-on-last-page rules of
 * validateActionState), Next/Previous navigation incl. page enter/exit callbacks, settings
 * collection via DFS (id key, generated `page_.setting_N` keys, nested containers, nodes without
 * extractors), Previous on empty history, WizardPane default callbacks, show()+cancel and
 * showAndWait()+cancel.
 *
 * Not covered: Dialog rendering details (owned by JavaFX).
 *
 * @author K
 * @since 1.0.0
 */
internal class WizardTest {

    /** WizardPane subclass recording enter/exit callback invocations. */
    private class TrackingPage : Wizard.WizardPane() {
        var enterCount = 0
        var exitCount = 0
        var lastWizard: Wizard? = null

        override fun onEnteringPage(wizard: Wizard?) {
            enterCount++
            lastWizard = wizard
        }

        override fun onExitingPage(wizard: Wizard?) {
            exitCount++
        }
    }

    private fun button(page: Wizard.WizardPane, data: ButtonData): Button {
        val type = page.buttonTypes.first { it.buttonData == data }
        return page.lookupButton(type) as Button
    }

    @Test
    fun defaultConstructorInstallsLinearFlowAndEmptyState() = runFx {
        val wizard = Wizard()

        assertTrue(wizard.getFlow() is LinearWizardFlow)
        assertSame(wizard.getFlow(), wizard.flowProperty().get())
        assertTrue(wizard.settings.isEmpty())
        assertFalse(wizard.hasProperties())
        assertNull(wizard.userData)
        // userData getter materialized the (still empty) properties map
        assertFalse(wizard.hasProperties())
    }

    @Test
    fun userDataIsStoredInPropertiesMap() = runFx {
        val wizard = Wizard("title")

        wizard.setUserData("payload")

        assertEquals("payload", wizard.userData)
        assertTrue(wizard.hasProperties())
        assertEquals(1, wizard.getProperties()!!.size)
    }

    @Test
    fun setFlowEntersFirstPageAndInstallsButtons() = runFx {
        val p1 = TrackingPage()
        val p2 = TrackingPage()
        val wizard = Wizard()

        wizard.setFlow(LinearWizardFlow(p1, p2))

        assertEquals(1, p1.enterCount)
        assertSame(wizard, p1.lastWizard)
        assertEquals(0, p2.enterCount)

        val datas = p1.buttonTypes.map { it.buttonData }
        assertTrue(ButtonData.BACK_PREVIOUS in datas)
        assertTrue(ButtonData.CANCEL_CLOSE in datas)
        // more pages remain: Next must be the first (default) button, Finish removed
        assertEquals(ButtonData.NEXT_FORWARD, p1.buttonTypes.first().buttonData)
        assertFalse(ButtonType.FINISH in p1.buttonTypes)
    }

    @Test
    fun lastPageShowsFinishInsteadOfNext() = runFx {
        val only = TrackingPage()
        val wizard = Wizard()

        wizard.setFlow(LinearWizardFlow(only))

        assertTrue(ButtonType.FINISH in only.buttonTypes)
        assertFalse(only.buttonTypes.any { it.buttonData == ButtonData.NEXT_FORWARD })
        assertTrue(ButtonType.CANCEL in only.buttonTypes)
    }

    @Test
    fun nextCollectsSettingsAndEntersSecondPage() = runFx {
        val p1 = TrackingPage()
        val nameField = TextField("Kudos").apply { id = "firstName" }
        val agree = CheckBox().apply { isSelected = true }
        val ratio = Slider(0.0, 100.0, 42.0)
        p1.content = VBox(nameField, agree, Label("decoration"), HBox(ratio))
        val p2 = TrackingPage()
        val wizard = Wizard()
        wizard.setFlow(LinearWizardFlow(p1, p2))

        button(p1, ButtonData.NEXT_FORWARD).fire()

        // page callbacks
        assertEquals(1, p1.exitCount)
        assertEquals(1, p2.enterCount)
        // DFS settings collection: id key first, generated keys keep counting afterwards
        assertEquals("Kudos", wizard.settings["firstName"])
        assertEquals(true, wizard.settings["page_.setting_1"])
        assertEquals(42.0, wizard.settings["page_.setting_2"])
        assertEquals(3, wizard.settings.size)
        // second page is the last one -> Finish present, Next gone
        assertTrue(ButtonType.FINISH in p2.buttonTypes)
        assertFalse(p2.buttonTypes.any { it.buttonData == ButtonData.NEXT_FORWARD })
    }

    @Test
    fun previousReturnsToFirstPage() = runFx {
        val p1 = TrackingPage()
        val p2 = TrackingPage()
        val wizard = Wizard()
        wizard.setFlow(LinearWizardFlow(p1, p2))
        button(p1, ButtonData.NEXT_FORWARD).fire()
        assertEquals(1, p2.enterCount)

        button(p2, ButtonData.BACK_PREVIOUS).fire()

        assertEquals(2, p1.enterCount)
        // back at the first page: Next is again the leading button
        assertEquals(ButtonData.NEXT_FORWARD, p1.buttonTypes.first().buttonData)
    }

    @Test
    fun previousWithEmptyHistoryIsSafeNoOp() = runFx {
        val p1 = TrackingPage()
        val p2 = TrackingPage()
        val wizard = Wizard()
        wizard.setFlow(LinearWizardFlow(p1, p2))

        button(p1, ButtonData.BACK_PREVIOUS).fire()

        // no page change: no new enter callbacks fired
        assertEquals(1, p1.enterCount)
        assertEquals(0, p2.enterCount)
    }

    @Test
    fun settingNewFlowResetsToItsFirstPage() = runFx {
        val a1 = TrackingPage()
        val a2 = TrackingPage()
        val wizard = Wizard()
        wizard.setFlow(LinearWizardFlow(a1, a2))
        button(a1, ButtonData.NEXT_FORWARD).fire()

        val b1 = TrackingPage()
        // the current page (a2) is unknown to the new flow -> advance starts from its first page
        wizard.setFlow(LinearWizardFlow(b1))

        assertEquals(1, b1.enterCount)
        assertTrue(ButtonType.FINISH in b1.buttonTypes)
    }

    @Test
    fun nodesWithoutValueExtractorYieldNoSettings() = runFx {
        val p1 = TrackingPage()
        p1.content = VBox(Label("a"), Label("b"))
        val p2 = TrackingPage()
        val wizard = Wizard()
        wizard.setFlow(LinearWizardFlow(p1, p2))

        button(p1, ButtonData.NEXT_FORWARD).fire()

        assertTrue(wizard.settings.isEmpty())
    }

    @Test
    fun blankIdFallsBackToGeneratedSettingKey() = runFx {
        val p1 = TrackingPage()
        p1.content = VBox(TextField("v").apply { id = "" })
        val p2 = TrackingPage()
        val wizard = Wizard()
        wizard.setFlow(LinearWizardFlow(p1, p2))

        button(p1, ButtonData.NEXT_FORWARD).fire()

        assertEquals("v", wizard.settings["page_.setting_0"])
    }

    @Test
    fun wizardPaneDefaultCallbacksAreNoOps() = runFx {
        val pane = Wizard.WizardPane()
        // must not throw
        pane.onEnteringPage(null)
        pane.onExitingPage(null)
    }

    @Test
    fun showDisplaysDialogUntilCancelled() = runFx {
        val p1 = TrackingPage()
        val wizard = Wizard("show-test")
        wizard.setFlow(LinearWizardFlow(p1))
        val windowsBefore = Window.getWindows().count { it.isShowing }

        wizard.show()

        assertEquals(windowsBefore + 1, Window.getWindows().count { it.isShowing })
        (p1.lookupButton(ButtonType.CANCEL) as Button).fire()
        assertEquals(windowsBefore, Window.getWindows().count { it.isShowing })
    }

    @Test
    fun showAndWaitReturnsCancelResult() = runFx {
        val p1 = TrackingPage()
        val wizard = Wizard("show-and-wait-test")
        wizard.setFlow(LinearWizardFlow(p1))
        // processed inside the nested event loop opened by showAndWait
        Platform.runLater { (p1.lookupButton(ButtonType.CANCEL) as Button).fire() }

        val result = wizard.showAndWait()

        assertTrue(result.isPresent)
        assertEquals(ButtonType.CANCEL, result.get())
    }
}
