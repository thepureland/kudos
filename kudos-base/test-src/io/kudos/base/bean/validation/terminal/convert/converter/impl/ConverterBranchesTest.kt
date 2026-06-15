package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.support.Depends
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.base.bean.validation.terminal.convert.ConstraintConvertContext
import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotNull
import kotlin.reflect.full.memberProperties
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Branch-level tests for the impl converters. The pre-existing tests in this package read the
 * annotation from a *field* via `getDeclaredField(...).getAnnotation(...)`, but the constraints use
 * the `@get:` use-site target so they live on the getter; that lookup returns null and the test
 * bodies never execute, leaving the AND/OR branches, numeric branch, and `require` guards uncovered.
 * Here we read annotations from the property getter so the converters actually run.
 *
 * @author K
 * @since 1.0.0
 */
internal class ConverterBranchesTest {

    private inline fun <reified A : Annotation> annoOf(property: String): A =
        Beans::class.memberProperties.first { it.name == property }
            .getter.annotations.filterIsInstance<A>().first()

    private fun ctx(property: String) = ConstraintConvertContext(property, "", Beans::class)

    // ---------------- CompareConstraintConvertor ----------------

    @Test
    fun compareOnNumericPropertyAddsIsNumber() {
        val a = annoOf<Compare>("numericCompare")
        val rule = CompareConstraintConvertor(a).convert(ctx("numericCompare")).rule.single()
        assertEquals("true", rule["isNumber"])
        // depends has properties -> kept
        assertTrue(rule.containsKey("depends"))
    }

    @Test
    fun compareOnStringPropertyOmitsIsNumberAndDropsEmptyDepends() {
        val a = annoOf<Compare>("stringCompare")
        val rule = CompareConstraintConvertor(a).convert(ctx("stringCompare")).rule.single()
        assertFalse(rule.containsKey("isNumber"))
        // depends has no properties -> removed
        assertFalse(rule.containsKey("depends"))
    }

    @Test
    fun compareRequireRejectsNonCompareAnnotation() {
        val notCompare = annoOf<NotNull>("plain")
        val convertor = CompareConstraintConvertor(annoOf<Compare>("stringCompare"))
        // Force getRule with a wrong annotation type to hit the require() guard.
        convertor.convert(ctx("stringCompare")) // initialise context first
        val ex = assertFailsWith<IllegalArgumentException> { convertor.getRule(notCompare) }
        assertTrue(ex.message!!.contains("Compare"))
    }

    @Test
    fun compareListAnnotationProducesRulePerSubConstraint() {
        // Exercises the AbstractConstraintConvertor `.List` handling path: a @Compare.List wrapper
        // is iterated and each inner @Compare yields its own rule entry.
        val a = annoOf<Compare.List>("listCompare")
        val terminal = CompareConstraintConvertor(a).convert(ctx("listCompare"))
        assertEquals("Compare", terminal.constraint)
        assertEquals(2, terminal.rule.size)
    }

    // ---------------- ConstraintsConstraintConvertor ----------------

    @Test
    fun constraintsAndKeepsSubMessages() {
        val a = annoOf<Constraints>("constraintsAnd")
        val rule = ConstraintsConstraintConvertor(a).convert(ctx("constraintsAnd")).rule.single()
        assertFalse(rule.containsKey("andOr"))
        @Suppress("UNCHECKED_CAST")
        val notBlankRule = rule["NotBlank"] as Map<String, Any>
        assertTrue(notBlankRule.containsKey("message"))
    }

    @Test
    fun constraintsOrDropsSubMessagesAndAddsAndOr() {
        val a = annoOf<Constraints>("constraintsOr")
        val rule = ConstraintsConstraintConvertor(a).convert(ctx("constraintsOr")).rule.single()
        assertEquals(AndOrEnum.OR, rule["andOr"])
        assertTrue(rule.containsKey("message"))
        @Suppress("UNCHECKED_CAST")
        val notBlankRule = rule["NotBlank"] as Map<String, Any>
        assertFalse(notBlankRule.containsKey("message"))
    }

    @Test
    fun constraintsRequireRejectsNonConstraintsAnnotation() {
        val convertor = ConstraintsConstraintConvertor(annoOf<Constraints>("constraintsAnd"))
        convertor.convert(ctx("constraintsAnd"))
        val ex = assertFailsWith<IllegalArgumentException> { convertor.getRule(annoOf<NotNull>("plain")) }
        assertTrue(ex.message!!.contains("Constraints"))
    }

    // ---------------- MatchesConstraintConvertor ----------------

    @Test
    fun matchesUsesCustomMessageWhenProvided() {
        val a = annoOf<Matches>("matchesCustomMsg")
        val rule = MatchesConstraintConvertor(a).getRule(a)
        assertEquals("custom-msg", rule["message"])
        assertEquals(RegExpEnum.VAR_NAME.regex, rule["regexp"])
        assertEquals(0, rule["flags"])
    }

    @Test
    fun matchesFallsBackToDefaultMessageWhenBlank() {
        val a = annoOf<Matches>("matchesBlankMsg")
        val rule = MatchesConstraintConvertor(a).getRule(a)
        assertEquals(RegExpEnum.VAR_NAME.defaultMessageKey, rule["message"])
    }

    @Test
    fun matchesRequireRejectsNonMatchesAnnotation() {
        val convertor = MatchesConstraintConvertor(annoOf<Matches>("matchesBlankMsg"))
        val ex = assertFailsWith<IllegalArgumentException> { convertor.getRule(annoOf<NotNull>("plain")) }
        assertTrue(ex.message!!.contains("Matches"))
    }

    @Suppress("unused")
    internal data class Beans(
        @get:Compare(
            anotherProperty = "other",
            logic = LogicOperatorEnum.EQ,
            depends = Depends(properties = ["other"], values = ["x"])
        )
        val numericCompare: Int,

        @get:Compare(anotherProperty = "other", logic = LogicOperatorEnum.EQ)
        val stringCompare: String?,

        @get:Compare.List(
            Compare(anotherProperty = "a", logic = LogicOperatorEnum.GT),
            Compare(anotherProperty = "b", logic = LogicOperatorEnum.LT)
        )
        val listCompare: String?,

        @get:Constraints(
            order = [NotBlank::class],
            notBlank = NotBlank(message = "nb"),
            andOr = AndOrEnum.AND,
            message = "all"
        )
        val constraintsAnd: String?,

        @get:Constraints(
            order = [NotBlank::class],
            notBlank = NotBlank(message = "nb"),
            andOr = AndOrEnum.OR,
            message = "any"
        )
        val constraintsOr: String?,

        @get:Matches(value = RegExpEnum.VAR_NAME, message = "custom-msg")
        val matchesCustomMsg: String?,

        @get:Matches(value = RegExpEnum.VAR_NAME)
        val matchesBlankMsg: String?,

        @get:NotNull
        val plain: String?,
    )
}
