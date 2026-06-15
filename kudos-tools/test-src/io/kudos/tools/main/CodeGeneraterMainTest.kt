package io.kudos.tools.main

import javafx.application.Platform
import javafx.geometry.Pos
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.stage.Stage
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.*

/**
 * test for CodeGeneraterMain
 *
 * Covers start(): the JavaFX toolkit is booted once (Platform.startup) and start() is executed on
 * the FX application thread; the selection window's title, scene size, HBox layout (spacing and
 * centered alignment) and the two wizard buttons are verified, then the stage is closed.
 *
 * Not covered (recorded as uncovered): the buttons' action handlers (they launch the single-/multi-
 * table wizards whose showAndWait() blocks the FX thread waiting for user input) and the top-level
 * main() (Application.launch blocks until the application exits and may only be called once per JVM).
 *
 * @author K
 * @since 1.0.0
 */
internal class CodeGeneraterMainTest {

    @Test
    fun applicationIsInstantiableWithoutToolkit() {
        assertNotNull(CodeGeneraterMain())
    }

    @Test
    fun startBuildsSelectionWindowWithTwoWizardButtons() {
        startFxToolkit()
        val latch = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        Platform.runLater {
            try {
                val stage = Stage()
                CodeGeneraterMain().start(stage)
                assertEquals("Kudos Code Generator", stage.title)
                assertTrue(stage.isShowing, "start() must show the primary stage")
                val scene = stage.scene
                assertEquals(300.0, scene.width)
                assertEquals(100.0, scene.height)
                val root = assertIs<HBox>(scene.root)
                assertEquals(30.0, root.spacing)
                assertEquals(Pos.CENTER, root.alignment)
                val buttons = root.children.filterIsInstance<Button>()
                assertEquals(listOf("Single Table", "Multi Table"), buttons.map { it.text })
                assertTrue(buttons.all { it.onAction != null }, "both buttons must have an action handler")
                stage.close()
            } catch (t: Throwable) {
                failure.set(t)
            } finally {
                latch.countDown()
            }
        }
        assertTrue(latch.await(30, TimeUnit.SECONDS), "the JavaFX task did not complete in time")
        failure.get()?.let { throw it }
    }

    /** Boots the JavaFX toolkit once per JVM; subsequent calls are no-ops. */
    private fun startFxToolkit() {
        try {
            Platform.startup { }
        } catch (_: IllegalStateException) {
            // toolkit already running
        }
        Platform.setImplicitExit(false)
    }
}
