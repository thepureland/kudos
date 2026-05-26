package io.kudos.base.bean.validation.terminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Remote
import io.kudos.base.bean.validation.support.IBeanValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Test cases for RemoteConstraintConvertor.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class RemoteConstraintConvertorTest {

    @Test
    fun testGetRule() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(Remote::class.java)
        if (annotation != null) {
            val convertor = RemoteConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            assertNotNull(rule)
            // checkClass should be removed
            assertFalse(rule.containsKey("checkClass"))
        }
    }

    @Test
    fun testGetRuleRemovesCheckClass() {
        val annotation = TestBean::class.java.getDeclaredField("value")
            .getAnnotation(Remote::class.java)
        if (annotation != null) {
            val convertor = RemoteConstraintConvertor(annotation)
            val rule = convertor.getRule(annotation)
            // checkClass should not appear in the rule
            assertFalse(rule.containsKey("checkClass"))
            // Other attributes such as message should be present
            assertTrue(rule.containsKey("message"))
        }
    }

    data class TestBean(
        @get:Remote(
            requestUrl = "",
            checkClass = TestRemoteValidator::class,
            message = "remote validation failed",
        )
        val value: String?
    )

    class TestRemoteValidator : IBeanValidator<TestBean> {
        override fun validate(bean: TestBean): Boolean {
            return true
        }
    }
}
