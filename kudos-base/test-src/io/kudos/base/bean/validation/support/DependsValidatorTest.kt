package io.kudos.base.bean.validation.support

import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Test cases for DependsValidator.
 *
 * @author K
 * @since 1.0.0
 */
internal class DependsValidatorTest {

    @Test
    fun testValidate() {
        // Single condition
        assert(DependsValidator.validate(arrayOf("name1"), arrayOf("name1"), arrayOf(LogicOperatorEnum.EQ)))

        // Multi-condition "AND"
        assert(
            DependsValidator.validate(
                arrayOf("name1", "name2"), arrayOf("name1", "NAME2"), arrayOf(LogicOperatorEnum.EQ, LogicOperatorEnum.IEQ)
            )
        )

        // Multi-condition "OR"
        assert(
            DependsValidator.validate(
                arrayOf("name1", "name3"), arrayOf("name1", "NAME2"), arrayOf(LogicOperatorEnum.EQ, LogicOperatorEnum.IEQ),
                AndOrEnum.OR
            )
        )

        // With an array
        assert(DependsValidator.validate(arrayOf("name1"), arrayOf("[name2,name1,name3]"), arrayOf(LogicOperatorEnum.IN)))
    }

    @Test
    fun andReturnsFalseOnFirstFailure() {
        assertFalse(
            DependsValidator.validate(
                arrayOf("a", "b"), arrayOf("a", "X"), arrayOf(LogicOperatorEnum.EQ, LogicOperatorEnum.EQ)
            )
        )
    }

    @Test
    fun orReturnsFalseWhenAllConditionsFail() {
        assertFalse(
            DependsValidator.validate(
                arrayOf("a", "b"), arrayOf("X", "Y"), arrayOf(LogicOperatorEnum.EQ, LogicOperatorEnum.EQ),
                AndOrEnum.OR
            )
        )
    }

    @Test
    fun mismatchedArraySizesThrows() {
        val ex = assertFailsWith<IllegalStateException> {
            DependsValidator.validate(
                arrayOf("a", "b"), arrayOf("a"), arrayOf(LogicOperatorEnum.EQ)
            )
        }
        assertTrue(ex.message!!.contains("same size"))
    }

    @Test
    fun nullLeftValueIsComparedAsNullString() {
        // left value null -> toString() is null, EQ against "anything" should be false (not match)
        assertFalse(
            DependsValidator.validate(
                arrayOf<Any?>(null), arrayOf("x"), arrayOf(LogicOperatorEnum.EQ)
            )
        )
    }
}
