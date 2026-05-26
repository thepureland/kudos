package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import kotlin.test.Test

/**
 * Test cases for Constraints.
 *
 * @author K
 * @since 1.0.0
 */
internal class ConstraintsTest {

    @Test
    fun validate() {
        // AndOrEnum.AND: value is null; fails because of the NotNull constraint
        val bean1 = TestConstraintsBean(null)
        assert(ValidationKit.validateProperty(bean1, "captcha").isNotEmpty())

        // AndOrEnum.AND: one rule is violated; validation fails
        val bean2 = TestConstraintsBean("1234")
        assert(ValidationKit.validateProperty(bean2, "captcha").isNotEmpty())
//
//        // AndOrEnum.AND: a different rule is violated; validation fails
//        val bean3 = TestConstraintsBean("ABC")
//        assert(ValidationKit.validateProperty(bean3, "captcha").isNotEmpty())
//
//        // AndOrEnum.AND: in fail-fast mode, the reported message should be the one for the violated rule
//        val bean4 = TestConstraintsBean("1234567")
//        assertEquals("the captcha must consist of uppercase letters", ValidationKit.validateProperty(bean4, "captcha").first().message)
//
//        // AndOrEnum.AND: in non-fail-fast mode, all failure messages are returned; the order is not fixed
//        val bean5 = TestConstraintsBean("1234567")
//        var violations = ValidationKit.validateProperty(bean5, "captcha", failFast = false)
//        assertEquals(2, violations.size)
//
//        // AndOrEnum.AND: case where all rules pass
//        val bean6 = TestConstraintsBean("ABCDE")
//        assert(ValidationKit.validateProperty(bean6, "captcha").isEmpty())
//
//        // Test the Range constraint (actually composed of Min and Max)
//        assert(ValidationKit.validateValue(TestConstraintsBean::class, "age", null).isEmpty())
//        assert(ValidationKit.validateValue(TestConstraintsBean::class, "age", 19).isEmpty())
//        violations = ValidationKit.validateValue(TestConstraintsBean::class, "age", 17)
//        assertEquals("age must be between 18 and 60", violations.first().message)
//
//        // AndOrEnum.OR: when one rule holds
//        assert(ValidationKit.validateValue(TestConstraintsBean::class, "name", null).isEmpty())
//
//        // AndOrEnum.OR: when the other rule holds
//        assert(ValidationKit.validateValue(TestConstraintsBean::class, "name", "abc").isEmpty())
//
//        // AndOrEnum.OR: when all rules fail, the message reported is from Constraints
//        violations = ValidationKit.validateValue(TestConstraintsBean::class, "name", "ABC")
//        assertEquals("name validation failed", violations.first().message)
    }


    internal data class TestConstraintsBean(

        @get:Constraints(
            order = [NotNull::class, Pattern::class, Length::class],
            notNull = NotNull(),
            pattern = Pattern(regexp = "[A-Z]+", message = "the captcha must consist of uppercase letters"),
            length = Length(min = 4, max = 6, message = "the captcha length must be between 4 and 6")
        )
        val captcha: String?,

//        @get:Constraints(
//            range = Range(min = 18, max = 60, message = "age must be between 18 and 60")
//        )
//        val age: Int? = 19,

//        @get:Constraints(
//            andOr = AndOrEnum.OR,
//            beNull = Null(),
//            pattern = Pattern(regexp = "[a-z]+", message = "name must consist of lowercase letters"),
//            message = "name validation failed"
//        )
//        val name: String? = ""

    )

}
