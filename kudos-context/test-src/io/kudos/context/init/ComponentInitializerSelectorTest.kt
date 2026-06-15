package io.kudos.context.init

import org.springframework.core.type.AnnotationMetadata
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * test for ComponentInitializerSelector
 *
 * Coverage:
 * - selectImports scans the fixed kudos roots and returns initializer class names
 * - @EnableKudos(exclusions=[...]) removes the excluded initializer from the result (and logs it)
 * - A bootstrap class without @EnableKudos yields no exclusions
 *
 * @author K
 * @since 1.0.0
 */
internal class ComponentInitializerSelectorTest {

    /** Exposes the protected exclusion reader for direct assertions. */
    private class ExposingSelector : ComponentInitializerSelector() {
        fun exclusionsOf(metadata: AnnotationMetadata) = getExclusionComponentInitializer(metadata)
    }

    @EnableKudos(exclusions = [ValidatorAutoConfiguration::class])
    internal class AppWithExclusion

    internal class AppWithoutEnableKudos

    @Test
    fun selectImportsIncludesKnownInitializersWhenNothingExcluded() {
        val result = ComponentInitializerSelector()
            .selectImports(AnnotationMetadata.introspect(AppWithoutEnableKudos::class.java))
        assertTrue(ContextAutoConfiguration::class.qualifiedName in result)
        assertTrue(ValidatorAutoConfiguration::class.qualifiedName in result)
    }

    @Test
    fun selectImportsDropsExcludedInitializer() {
        val result = ComponentInitializerSelector()
            .selectImports(AnnotationMetadata.introspect(AppWithExclusion::class.java))
        assertFalse(
            ValidatorAutoConfiguration::class.qualifiedName in result,
            "the initializer excluded via @EnableKudos must not be imported"
        )
        assertTrue(
            ContextAutoConfiguration::class.qualifiedName in result,
            "non-excluded initializers must remain importable"
        )
    }

    @Test
    fun exclusionReaderReturnsDeclaredClasses() {
        val exclusions = ExposingSelector()
            .exclusionsOf(AnnotationMetadata.introspect(AppWithExclusion::class.java))
        assertEquals(listOf(ValidatorAutoConfiguration::class), exclusions)
    }

    @Test
    fun exclusionReaderReturnsEmptyListWithoutEnableKudos() {
        val exclusions = ExposingSelector()
            .exclusionsOf(AnnotationMetadata.introspect(AppWithoutEnableKudos::class.java))
        assertTrue(exclusions.isEmpty())
    }
}
