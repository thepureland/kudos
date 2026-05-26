package io.kudos.base.bean.validation.constraint

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.kit.ValidationKit
import jakarta.validation.ValidationException
import kotlin.test.Test
import kotlin.test.assertFailsWith

/**
 * Test cases for AtLeast.
 *
 * @author K
 * @since 1.0.0
 */
internal class AtLeastTest {

    /**
     * Test illegal-argument scenarios.
     */
    @Test
    fun testIllegalArguments() {
        val bean1 = TestIllegalArgumentsBean("")
        assertFailsWith<ValidationException> { ValidationKit.validateBean(bean1) }
    }

    /**
     * Test a single AtLeast.
     */
    @Test
    fun testAtLeast() {
        // Two values non-null: passes
        val bean1 = TestAtLeastBean("1", "2", null, null)
        assert(ValidationKit.validateBean(bean1).isEmpty())

        // Three values non-null: passes
        val bean2 = TestAtLeastBean("1", "2", "3", null)
        assert(ValidationKit.validateBean(bean2).isEmpty())

        // Only one value non-null: fails
        val bean3 = TestAtLeastBean("1", null, null, null)
        assert(ValidationKit.validateBean(bean3).isNotEmpty())
    }

    /**
     * Test multiple AtLeast constraints.
     */
    @Test
    fun testAtLeastList() {
        val bean1 = TestAtLeastListBean("1", null, null, "4")
        assert(ValidationKit.validateBean(bean1).isEmpty())
    }

    @AtLeast(properties = ["p1", "p2", "p3"], count = 4, message = "at least four of p1, p2, p3 must be non-null")
    internal data class TestIllegalArgumentsBean(
        val p: String
    )

    @AtLeast(properties = ["p1", "p2", "p3"], count = 2, message = "at least two of p1, p2, p3 must be non-null")
    internal data class TestAtLeastBean(

        val p1: String?,

        val p2: String?,

        val p3: String?,

        val p4: String?

    )

    @AtLeast.List(
        AtLeast(properties = ["p1", "p2"], message = "at least one of p1, p2 must be non-null"),
        AtLeast(properties = ["p3", "p4"], message = "at least one of p3, p4 must be non-null")
    )
    internal data class TestAtLeastListBean(

        val p1: String?,

        val p2: String?,

        val p3: String?,

        val p4: String?

    )

}
