package io.kudos.test.common.init

import io.kudos.context.init.EnableKudos
import kotlin.reflect.full.hasAnnotation
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Unit tests for [TestApplication].
 *
 * The class is just a bootstrap marker annotated with [EnableKudos]. Tests verify it is instantiable (the
 * class is `open` with a no-arg constructor) and that it carries the [EnableKudos] meta-annotation with the
 * default empty exclusions. Reflection-only; no Spring container is started.
 *
 * @author K
 * @since 1.0.0
 */
internal class TestApplicationTest {

    @Test
    fun instantiable() {
        val instance = TestApplication()
        assertNotNull(instance)
    }

    @Test
    fun carriesEnableKudosAnnotation() {
        assertTrue(TestApplication::class.hasAnnotation<EnableKudos>())
        val ann = TestApplication::class.annotations.filterIsInstance<EnableKudos>().single()
        assertTrue(ann.exclusions.isEmpty(), "default exclusions should be empty")
    }
}
