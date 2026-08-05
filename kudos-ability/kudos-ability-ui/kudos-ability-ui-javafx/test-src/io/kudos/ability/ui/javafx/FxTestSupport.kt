package io.kudos.ability.ui.javafx

import javafx.application.Platform
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

/**
 * Shared JavaFX test bootstrap: boots the toolkit once per JVM and runs test bodies on the
 * FX application thread, rethrowing any assertion error on the JUnit thread.
 *
 * @author K
 * @since 1.0.0
 */
internal object FxTestSupport {

    /** Boots the JavaFX toolkit once per JVM; subsequent calls are no-ops. */
    private fun ensureStarted() {
        try {
            Platform.startup { }
        } catch (_: IllegalStateException) {
            // toolkit already running
        }
        Platform.setImplicitExit(false)
    }

    /**
     * Runs [block] on the FX application thread and waits for completion.
     * Any throwable raised inside the block is rethrown on the calling (JUnit) thread.
     */
    fun runFx(block: () -> Unit) {
        ensureStarted()
        val latch = CountDownLatch(1)
        val failure = AtomicReference<Throwable?>()
        Platform.runLater {
            try {
                block()
            } catch (t: Throwable) {
                failure.set(t)
            } finally {
                latch.countDown()
            }
        }
        check(latch.await(120, TimeUnit.SECONDS)) { "FX task timed out" }
        failure.get()?.let { throw it }
    }
}
