package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.DictEnumItemCode
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.enums.ienums.IDictEnum
import kotlin.test.Test
import kotlin.test.assertFalse

/**
 * Test cases for DictEnumItemCode.
 *
 * @author K
 * @since 1.0.0
 */
internal class DictEnumItemCodeTest {

    @Test
    fun validate() {
        val bean1 = TestDictEnumCodeBean("1")
        assert(ValidationKit.validateBean(bean1).isEmpty())

        val bean2 = TestDictEnumCodeBean("4")
        assertFalse(ValidationKit.validateBean(bean2).isEmpty())
    }

    internal data class TestDictEnumCodeBean(

        @get:DictEnumItemCode(enumClass = TestEnum::class, message = "value must be one of the codes defined in TestEnum")
        val elemCode: String?

    )

    internal enum class TestEnum(override val code: String, override var displayText: String): IDictEnum {

        ELEM1("1", "Element 1"),
        ELEM2("2", "Element 2"),
        ELEM3("3", "Element 3")

    }

}
