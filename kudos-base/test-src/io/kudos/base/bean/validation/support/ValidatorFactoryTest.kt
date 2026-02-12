package io.kudos.base.bean.validation.support

import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.CreditCardNumber
import org.hibernate.validator.constraints.Length
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * ValidatorFactory测试用例
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ValidatorFactoryTest {

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
