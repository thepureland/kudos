package io.kudos.base.bean.validation.teminal.convert.converter.impl

import io.kudos.base.bean.validation.constraint.annotations.Remote
import io.kudos.base.bean.validation.support.IBeanValidator
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * RemoteConstraintConvertor测试用例
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
            // checkClass应该被移除
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
            // checkClass不应该在规则中
            assertFalse(rule.containsKey("checkClass"))
            // 应该包含其他属性如message
            assertTrue(rule.containsKey("message"))
        }
    }

    data class TestBean(
        @get:Remote(
            requestUrl = "",
            checkClass = TestRemoteValidator::class,
            message = "远程验证失败",
        )
        val value: String?
    )

    class TestRemoteValidator : IBeanValidator<TestBean> {
        override fun validate(bean: TestBean): Boolean {
            return true
        }
    }
}
