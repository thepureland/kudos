package io.kudos.base.bean.validation.support

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Forces class initialization and reads every declared constant of all [RegExps] inner objects.
 *
 * Most [RegExps] constants are `const val`s that get inlined at their (test) call sites, so the inner
 * objects' static initializers are never executed and their per-constant lines stay uncovered. Reading
 * each field reflectively triggers the object class `<clinit>` and touches every constant, and also
 * pins the regex constants as valid, compilable patterns.
 *
 * @author K
 * @since 1.0.0
 */
internal class RegExpsLoadAllTest {

    private val innerObjects = listOf(
        RegExps.Communication::class.java,
        RegExps.Name::class.java,
        RegExps.Text::class.java,
        RegExps.CharacterSet::class.java,
        RegExps.Security::class.java,
        RegExps.Numeric::class.java,
        RegExps.Network::class.java,
        RegExps.Business::class.java,
        RegExps.Format::class.java,
    )

    @Test
    fun everyConstantIsReadableAndIsValidRegex() {
        var count = 0
        for (clazz in innerObjects) {
            // Touch the INSTANCE field too, ensuring the object's <clinit> runs.
            val instanceField = clazz.getField("INSTANCE")
            val instance = instanceField.get(null)
            assertTrue(instance != null)

            for (field in clazz.declaredFields) {
                if (field.type == String::class.java) {
                    field.isAccessible = true
                    val value = field.get(instance) as? String ?: field.get(null) as? String
                    if (value != null) {
                        // Every constant must compile as a regex.
                        Regex(value)
                        count++
                    }
                }
            }
        }
        assertTrue(count > 0, "expected to read at least one regex constant")
    }
}
