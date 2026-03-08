package io.kudos.base.bean.validation.constraint.validator

import io.kudos.base.bean.validation.constraint.annotations.DictEnumItemCode
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.enums.ienums.IDictEnum
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * DictEnumCodeValidator测试用例
 *
 * @author K
 * @since 1.0.0
 */
internal class DictEnumItemCodeValidatorTest {

    @Test
    fun validate() {
        val bean1 = TestDictEnumCodeBean("1")
        assert(ValidationKit.validateBean(bean1).isEmpty())

        val bean2 = TestDictEnumCodeBean("4")
        assertFalse(ValidationKit.validateBean(bean2).isEmpty())
    }

    internal data class TestDictEnumCodeBean(

        @get:DictEnumItemCode(enumClass = TestEnum::class, message = "值必须在枚举TestEnum的代码中")
        val elemCode: String?

    )

    internal enum class TestEnum(override val code: String, override var trans: String): IDictEnum {

        ELEM1("1", "元素1"),
        ELEM2("2", "元素2"),
        ELEM3("3", "元素3")

    }

}