package io.kudos.base.bean.validation.support

import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import kotlin.test.Test

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

}
