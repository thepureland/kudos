package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.Constraints
import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.support.logic.AndOrEnum
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Null
import jakarta.validation.constraints.Pattern
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.Range
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Test cases for Constraints.
 *
 * Coverage:
 * - AndOrEnum.AND: null value, single rule violations, fail-fast vs non-fail-fast,
 *   the all-pass case, and explicit order with NotNull priority promotion.
 * - Composite Range sub-constraint (Min + Max).
 * - AndOrEnum.OR: pass via either rule, and the all-fail case reporting the Constraints message.
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

        // AndOrEnum.AND: a different rule is violated; validation fails
        val bean3 = TestConstraintsBean("ABC")
        assert(ValidationKit.validateProperty(bean3, "captcha").isNotEmpty())

        // AndOrEnum.AND: in fail-fast mode, the reported message should be the one for the violated rule
        val bean4 = TestConstraintsBean("1234567")
        assertEquals(
            "the captcha must consist of uppercase letters",
            ValidationKit.validateProperty(bean4, "captcha").first().message
        )

        // AndOrEnum.AND: in non-fail-fast mode, all failure messages are returned; the order is not fixed
        val bean5 = TestConstraintsBean("1234567")
        var violations = ValidationKit.validateProperty(bean5, "captcha", failFast = false)
        assertEquals(2, violations.size)

        // AndOrEnum.AND: case where all rules pass
        val bean6 = TestConstraintsBean("ABCDE")
        assert(ValidationKit.validateProperty(bean6, "captcha").isEmpty())

        // Composite Range sub-constraint (Min + Max): a value inside [10, 20] passes.
        // Regression guard for B7: ConstraintsValidator used to re-initialize the expanded
        // Min/Max validators with the raw Range annotation, throwing ClassCastException. It must
        // now validate without throwing.
        val beanRangeOk = TestConstraintsBean("ABCDE", "abc", 15)
        assert(ValidationKit.validateProperty(beanRangeOk, "score").isEmpty())

        // Composite Range sub-constraint: a value below the lower bound fails (no CCE).
        val beanRangeLow = TestConstraintsBean("ABCDE", "abc", 5)
        assert(ValidationKit.validateProperty(beanRangeLow, "score").isNotEmpty())

        // Composite Range sub-constraint: a value above the upper bound fails (no CCE).
        val beanRangeHigh = TestConstraintsBean("ABCDE", "abc", 25)
        assert(ValidationKit.validateProperty(beanRangeHigh, "score").isNotEmpty())

        // AndOrEnum.OR: when one rule holds
        assert(ValidationKit.validateValue(TestConstraintsBean::class, "name", null).isEmpty())

        // AndOrEnum.OR: when the other rule holds
        assert(ValidationKit.validateValue(TestConstraintsBean::class, "name", "abc").isEmpty())

        // AndOrEnum.OR: when all rules fail, the message reported is from Constraints
        violations = ValidationKit.validateValue(TestConstraintsBean::class, "name", "ABC")
        assertEquals("name validation failed", violations.first().message)
    }


    internal data class TestConstraintsBean(

        @get:Constraints(
            order = [NotNull::class, Pattern::class, Length::class],
            notNull = NotNull(),
            pattern = Pattern(regexp = "[A-Z]+", message = "the captcha must consist of uppercase letters"),
            length = Length(min = 4, max = 6, message = "the captcha length must be between 4 and 6")
        )
        val captcha: String?,

        @get:Constraints(
            andOr = AndOrEnum.OR,
            beNull = Null(),
            pattern = Pattern(regexp = "[a-z]+", message = "name must consist of lowercase letters"),
            message = "name validation failed"
        )
        val name: String? = "",

        @get:Constraints(
            range = Range(min = 10, max = 20, message = "score must be between 10 and 20")
        )
        val score: Int? = null

    )

}
