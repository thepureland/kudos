package io.kudos.base.bean.validation.support

import io.kudos.base.support.logic.AndOrEnum
import io.kudos.base.support.logic.LogicOperatorEnum
import kotlin.test.Test

/**
 * DependsValidator测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class DependsValidatorTest {

    @Test
    fun testValidate() {
        // 单条件
        assert(DependsValidator.validate(arrayOf("name1"), arrayOf("name1"), arrayOf(LogicOperatorEnum.EQ)))

        // 多条件"与"
        assert(
            DependsValidator.validate(
                arrayOf("name1", "name2"), arrayOf("name1", "NAME2"), arrayOf(LogicOperatorEnum.EQ, LogicOperatorEnum.IEQ)
            )
        )

        // 多条件"或"
        assert(
            DependsValidator.validate(
                arrayOf("name1", "name3"), arrayOf("name1", "NAME2"), arrayOf(LogicOperatorEnum.EQ, LogicOperatorEnum.IEQ),
                AndOrEnum.OR
            )
        )

        // 含数组
        assert(DependsValidator.validate(arrayOf("name1"), arrayOf("[name2,name1,name3]"), arrayOf(LogicOperatorEnum.IN)))
    }

}