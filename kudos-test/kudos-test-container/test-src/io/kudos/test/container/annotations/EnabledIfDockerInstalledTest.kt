package io.kudos.test.container.annotations

import org.junit.jupiter.api.extension.ExtendWith
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Verifies the [EnabledIfDockerInstalled] meta-annotation wiring: retention, targets, and the
 * registered [ExtendWith] extension being [DockerInstalledCondition].
 *
 * @author K
 * @since 1.0.0
 */
internal class EnabledIfDockerInstalledTest {

    @Test
    fun `is meta-annotated with ExtendWith DockerInstalledCondition`() {
        val extendWith = EnabledIfDockerInstalled::class.java.getAnnotation(ExtendWith::class.java)
        assertNotNull(extendWith)
        assertTrue(extendWith.value.any { it == DockerInstalledCondition::class })
    }

    @Test
    fun `has runtime retention`() {
        val retention = EnabledIfDockerInstalled::class.java.getAnnotation(Retention::class.java)
        // Kotlin's RUNTIME retention maps to Java RUNTIME so the annotation is reflectively visible above.
        assertEquals(AnnotationRetention.RUNTIME, retention.value)
    }

    @Test
    fun `targets class and function`() {
        val target = EnabledIfDockerInstalled::class.java.getAnnotation(Target::class.java)
        assertNotNull(target)
        assertTrue(target.allowedTargets.contains(AnnotationTarget.CLASS))
        assertTrue(target.allowedTargets.contains(AnnotationTarget.FUNCTION))
    }
}
