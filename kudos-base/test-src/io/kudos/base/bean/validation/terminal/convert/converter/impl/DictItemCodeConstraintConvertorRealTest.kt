package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.DictItemCode
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real-execution tests for [DictCodeConstraintConvertor].
 *
 * The existing [DictItemCodeConstraintConvertorTest] reads the annotation from a *field* via
 * `getDeclaredField(...).getAnnotation(...)`, but the constraint is declared with the `@get:` use-site
 * target so it lives on the getter, not the field; that lookup returns null and the test body is skipped,
 * leaving the converter uncovered. Here we read the annotation from the property getter so the converter
 * actually runs, and we rely on the test-only SPI [io.kudos.base.bean.validation.constraint.TestDictItemCodeFinder]
 * (registered via META-INF/services) for deterministic dictionary codes.
 *
 * @author K
 * @since 1.0.0
 */
internal class DictItemCodeConstraintConvertorRealTest {

    private fun dictItemCodeAnno(): DictItemCode =
        TestBean::class.memberProperties.first { it.name == "code" }
            .getter.annotations.filterIsInstance<DictItemCode>().first()

    @Test
    fun getRulePopulatesValuesFromSpiFinder() {
        val annotation = dictItemCodeAnno()
        val convertor = DictCodeConstraintConvertor(annotation)
        val rule = convertor.getRule(annotation)
        assertTrue(rule.containsKey("values"))
        @Suppress("UNCHECKED_CAST")
        val values = rule["values"] as Set<String>
        assertEquals(setOf("VALID_CODE", "ANOTHER_VALID_CODE"), values)
    }

    @Test
    fun getRuleReturnsEmptyValuesForUnknownDict() {
        val annotation = TestBean::class.memberProperties.first { it.name == "unknown" }
            .getter.annotations.filterIsInstance<DictItemCode>().first()
        val convertor = DictCodeConstraintConvertor(annotation)
        val rule = convertor.getRule(annotation)
        @Suppress("UNCHECKED_CAST")
        val values = rule["values"] as Set<String>
        assertTrue(values.isEmpty())
    }

    @Test
    fun convertProducesTerminalConstraintWithDictItemCodeName() {
        val annotation = dictItemCodeAnno()
        val ctx = ConstraintConvertContext("code", "", TestBean::class)
        val terminal = DictCodeConstraintConvertor(annotation).convert(ctx)
        assertEquals("DictItemCode", terminal.constraint)
        assertEquals("code", terminal.prop)
        assertTrue(terminal.rule.single().containsKey("values"))
    }

    internal data class TestBean(
        @get:DictItemCode(atomicServiceCode = "test", dictType = "test", message = "invalid dict code")
        val code: String?,

        @get:DictItemCode(atomicServiceCode = "nope", dictType = "nope")
        val unknown: String?,
    )
}
