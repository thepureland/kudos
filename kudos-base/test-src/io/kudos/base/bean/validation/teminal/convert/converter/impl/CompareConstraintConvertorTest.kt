package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.support.Depends
import io.kudos.base.bean.validation.teminal.convert.ConstraintConvertContext
import io.kudos.base.support.logic.LogicOperatorEnum
import kotlin.reflect.full.memberProperties
import kotlin.test.Test

/**
 * CompareConstraintConvertor测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class CompareConstraintConvertorTest {

    @Test
    fun test() {
        val context = ConstraintConvertContext("confirmPassword", null, CompareTestBean::class)
        val prop = CompareTestBean::class.memberProperties.first { it.name == "confirmPassword" }
        val annotation = prop.getter.annotations.first()
        val teminalConstraint = CompareConstraintConvertor(annotation).convert(context)
        println(teminalConstraint)

        val context1 = ConstraintConvertContext("medium", null, CompareTestBean::class)
        val prop1 = CompareTestBean::class.memberProperties.first { it.name == "medium" }
        val annotation1 = prop1.getter.annotations.first()
        val teminalConstraint1 = CompareConstraintConvertor(annotation1).convert(context1)
        println(teminalConstraint1)

    }

    internal data class CompareTestBean(
        val validate: Boolean?,

        val password: String?,

        @get:Compare(
            depends = Depends(
                properties = ["validate"],
                values = ["true"]
            ),
            anotherProperty = "password",
            logic = LogicOperatorEnum.EQ,
            message = "两次密码不同"
        )
        val confirmPassword: String?,


        @get:Compare.List(
            Compare(
                anotherProperty = "small",
                logic = LogicOperatorEnum.GT,
                message = "medium必须大于small"
            ),
            Compare(
                anotherProperty = "large",
                logic = LogicOperatorEnum.LT,
                message = "medium必须小于large"
            )
        )
        val medium: String?
    )

}