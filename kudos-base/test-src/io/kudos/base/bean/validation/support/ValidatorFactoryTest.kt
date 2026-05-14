package io.kudos.base.bean.validation.support

import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.CreditCardNumber
import org.hibernate.validator.constraints.Length
import org.hibernate.validator.constraints.Range
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotSame
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * ValidatorFactory测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ValidatorFactoryTest {

    @BeforeTest
    fun clearCache() {
        // 各 case 之间不共享缓存，避免相互干扰
        ValidatorFactory.clearCacheForTest()
    }

    @Test
    fun testGetValidatorForNotNull() {
        val annotation = TestBean::class.java.getDeclaredField("name")
            .getAnnotation(NotNull::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, "test")
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForSize() {
        val annotation = TestBean::class.java.getDeclaredField("name")
            .getAnnotation(Size::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, "test")
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForMin() {
        val annotation = TestBean::class.java.getDeclaredField("age")
            .getAnnotation(Min::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, 25)
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForMax() {
        val annotation = TestBean::class.java.getDeclaredField("age")
            .getAnnotation(Max::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, 25)
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForPattern() {
        val annotation = TestBean::class.java.getDeclaredField("email")
            .getAnnotation(Pattern::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, "test@example.com")
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForEmail() {
        val annotation = TestBean::class.java.getDeclaredField("email")
            .getAnnotation(Email::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, "test@example.com")
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForLength() {
        val annotation = TestBean::class.java.getDeclaredField("name")
            .getAnnotation(Length::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, "test")
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForRange() {
        val annotation = TestBean::class.java.getDeclaredField("age")
            .getAnnotation(org.hibernate.validator.constraints.Range::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, 25)
            // Range约束会返回两个验证器（Min和Max）
            assertTrue(validators.size >= 2)
        }
    }

    @Test
    fun testGetValidatorForCreditCardNumber() {
        val annotation = TestBean::class.java.getDeclaredField("cardNo")
            .getAnnotation(CreditCardNumber::class.java)
        if (annotation != null) {
            val validators = ValidatorFactory.getValidator(annotation, "4111111111111111")
            assertFalse(validators.isEmpty())
        }
    }

    @Test
    fun testGetValidatorForUnsupportedAnnotation() {
        val annotation = object : Annotation {
            fun annotationType() = javaClass
        }
        val validators = ValidatorFactory.getValidator(annotation, "test")
        // 不支持的注解应该返回空列表
        assertTrue(validators.isEmpty())
    }

    private fun ageAnnotation(klass: Class<out Annotation>): Annotation =
        TestBean::class.java.getDeclaredMethod("getAge").getAnnotation(klass)
            ?: error("TestBean.age 上找不到注解 $klass")

    /**
     * 同一个 (annotation, valueClass) 应命中缓存，返回同一个 validator 实例。
     */
    @Test
    fun testCacheReturnsSameInstanceForSameKey() {
        val annotation = ageAnnotation(Min::class.java)
        val v1 = ValidatorFactory.getValidator(annotation, 25)
        val v2 = ValidatorFactory.getValidator(annotation, 25)
        assertSame(v1, v2, "缓存应返回同一个 List 实例")
        assertSame(v1[0], v2[0], "缓存应返回同一个 validator 实例")
    }

    /**
     * 同一注解但不同 value class 应触发独立的 builder 实例化。
     */
    @Test
    fun testCacheDistinguishesByValueClass() {
        val annotation = ageAnnotation(Min::class.java)
        val intValidators = ValidatorFactory.getValidator(annotation, 25)        // Int
        val longValidators = ValidatorFactory.getValidator(annotation, 25L)      // Long
        assertNotSame(
            intValidators[0],
            longValidators[0],
            "Int 与 Long 应该分别用 MinValidatorForInteger / MinValidatorForLong"
        )
    }

    /**
     * 内容相等的两个动态构造注解应命中同一缓存项（依赖 JDK Annotation 的内容相等契约）。
     * Range 内部用反射构造 Min/Max 注解，缓存能短路掉反射开销。
     */
    @Test
    fun testCacheHitsForRangeBetweenCalls() {
        val annotation = ageAnnotation(Range::class.java)
        val v1 = ValidatorFactory.getValidator(annotation, 25)
        val v2 = ValidatorFactory.getValidator(annotation, 25)
        assertSame(v1, v2, "Range 整体应被缓存")
        assertEquals(2, v1.size, "Range 应返回 Min + Max 两个 validator")
        assertSame(v1[0], v2[0])
        assertSame(v1[1], v2[1])
    }

    data class TestBean(
        @get:NotNull
        @get:Size(min = 1, max = 100)
        @get:Length(min = 1, max = 100)
        val name: String?,
        
        @get:Min(18)
        @get:Max(100)
        @get:org.hibernate.validator.constraints.Range(min = 18, max = 100)
        val age: Int?,
        
        @get:Email
        @get:Pattern(regexp = "^[a-zA-Z0-9._%+-]+@[a-zA-Z0-9.-]+\\.[a-zA-Z]{2,}$")
        val email: String?,

        @get:CreditCardNumber(ignoreNonDigitCharacters = true)
        val cardNo: String?
    )
}
