package io.kudos.base.bean.validation

import io.kudos.base.bean.validation.kit.ValidationKit
import io.kudos.base.bean.validation.support.AbstractGroupSequenceProvider
import io.kudos.base.bean.validation.support.Group
import jakarta.validation.GroupSequence
import jakarta.validation.constraints.Max
import jakarta.validation.constraints.Min
import jakarta.validation.constraints.NotNull
import jakarta.validation.constraints.Pattern
import jakarta.validation.groups.Default
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.group.GroupSequenceProvider
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Group validation test cases.
 *
 * @author K
 * @since 1.0.0
 */
internal class GroupValidationTest {

    /**
     * Tests group filtering.
     */
    @Test
    fun testFilterGroup() {
        // Only the Group.First::class group
        val bean1 = TestFilterGroupBean(null, null)
        val violations1 = ValidationKit.validateBean(bean1)
        assertEquals("name must not be null", violations1.first().message)
        //// With @GroupSequence, even explicitly passing Default::class has no effect (@GroupSequence has higher priority)
        val violations2 = ValidationKit.validateBean(bean1, Default::class)
        assertEquals("name must not be null", violations2.first().message)
    }

    /**
     * Tests the validation order of constraints within the same group: all are unordered.
     */
    @Test
    fun testDefaultOrderSameGroupBean() {
        // Constraint validation on a single property is unordered
        val msgSet = mutableSetOf<String>()
        for (i in 1 until 100) {
            val bean = TestValidateOrderSameGroupBean("123", "456", 61)
            val violations = ValidationKit.validateProperty(bean, "name", failFast = false)
            assertEquals(2, violations.size)
            msgSet.add(violations.first().message)
        }
        assertEquals(2, msgSet.size)

        // Constraint validation across properties within the same group is also unordered
        msgSet.clear()
        for (i in 1 until 100) {
            val bean = TestValidateOrderSameGroupBean("123", "456", 61)
            val violations = ValidationKit.validateBean(bean, Group.First::class, failFast = false)
            assertEquals(3, violations.size)
            msgSet.add(violations.first().message)
        }
        assertEquals(3, msgSet.size)
    }

    /**
     * Tests group ordering.
     */
    @Test
    fun testGroupSequence() {
        for (i in 1 until 10) {
            val bean1 = TestGroupSequenceBean("123", 61)
            val violations1 = ValidationKit.validateBean(bean1, failFast = false) // Non-fail-fast mode is ignored
            assertEquals(1, violations1.size) // Validation aborts as soon as the Group.First group fails (short-circuit effect of the group sequence)
            assertEquals("Must be under 60 years old", violations1.first().message)
        }
    }

    /**
     * Tests the group sequence provider.
     */
    @Test
    fun testGroupSequenceProvider() {
        for (i in 1 until 10) {
            // First the Group.First group, then the Group.Second group
            val bean1 = TestGroupSequenceProviderBean("123", 61)
            val violations1 = ValidationKit.validateBean(bean1, failFast = false)
            assertEquals(1, violations1.size)
            assertEquals("Must be under 60 years old", violations1.first().message)

            // Only the Group.Second group
            val bean2 = TestGroupSequenceProviderBean("456", 61)
            val violations2 = ValidationKit.validateBean(bean2, failFast = false)
            assertEquals(2, violations2.size)
        }
    }

    @GroupSequence(Group.First::class, TestFilterGroupBean::class) //!!! The current class must be included
    internal data class TestFilterGroupBean(

        @get:NotNull(message = "name must not be null", groups = [Group.First::class])
        val name: String?,

        @get:NotNull(message = "age must not be null") // When group is omitted, defaults to javax.validation.groups.Default::class
        val age: Int?
    )

    internal data class TestValidateOrderSameGroupBean(

        @get:Length(min = 6, max = 32, message = "name length must be between 6 and 32")
        @get:Pattern(regexp = "[a-zA-Z]+", message = "name must consist of letters")
        val name: String?,

        @get:Length(min = 6, max = 32, message = "name length must be between 6 and 32", groups = [Group.First::class])
        @get:Pattern(regexp = "[a-zA-Z]+", message = "name must consist of letters", groups = [Group.First::class])
        val name2: String?,

        @get:Max(60, message = "Must be under 60 years old", groups = [Group.First::class])
        @get:Min(18, message = "Must be at least 18 years old", groups = [Group.First::class])
        val age: Int?
    )

    @GroupSequence(Group.First::class, Group.Second::class, TestGroupSequenceBean::class) //!!! The current class must be included
    internal data class TestGroupSequenceBean(

        @get:Length(min = 6, max = 32, message = "name length must be between 6 and 32", groups = [Group.Second::class])
        @get:Pattern(regexp = "[a-zA-Z]+", message = "name must consist of letters", groups = [Group.Second::class])
        val name: String?,

        @get:Max(60, message = "Must be under 60 years old", groups = [Group.First::class])
        @get:Min(18, message = "Must be at least 18 years old", groups = [Group.First::class])
        val age: Int?
    )

    @GroupSequenceProvider(TestGroupSequenceProviderBean.GroupSequenceProvider::class)
    internal data class TestGroupSequenceProviderBean(

        @get:Length(min = 6, max = 32, message = "name length must be between 6 and 32", groups = [Group.Second::class])
        @get:Pattern(regexp = "[a-zA-Z]+", message = "name must consist of letters", groups = [Group.Second::class])
        val name: String?,

        @get:Max(60, message = "Must be under 60 years old", groups = [Group.First::class])
        @get:Min(18, message = "Must be at least 18 years old", groups = [Group.First::class])
        val age: Int?
    ) {
        class GroupSequenceProvider : AbstractGroupSequenceProvider<TestGroupSequenceProviderBean>() {

            override fun getGroups(bean: TestGroupSequenceProviderBean): List<KClass<*>> {
                val defaultGroupSequence = mutableListOf<KClass<*>>()
                if (bean.name == "123") {
                    defaultGroupSequence.add(Group.First::class)
                    defaultGroupSequence.add(Group.Second::class)
                } else {
                    defaultGroupSequence.add(Group.Second::class)
                }
                return defaultGroupSequence
            }

        }
    }


}