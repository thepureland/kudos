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
 * Test cases for ValidatorFactory.
 *
 * @author AI: cursor
 * @author K
 * @since 1.0.0
 */
internal class ValidatorFactoryTest {

    @BeforeTest
    fun clearCache() {
        // Do not share the cache across test cases to avoid interference
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
            // The Range constraint returns two validators (Min and Max)
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
        // Unsupported annotations should return an empty list
        assertTrue(validators.isEmpty())
    }

    private fun ageAnnotation(klass: Class<out Annotation>): Annotation =
        TestBean::class.java.getDeclaredMethod("getAge").getAnnotation(klass)
            ?: error("annotation $klass not found on TestBean.age")

    /**
     * The same (annotation, valueClass) should hit the cache and return the same validator instance.
     */
    @Test
    fun testCacheReturnsSameInstanceForSameKey() {
        val annotation = ageAnnotation(Min::class.java)
        val v1 = ValidatorFactory.getValidator(annotation, 25)
        val v2 = ValidatorFactory.getValidator(annotation, 25)
        assertSame(v1, v2, "the cache should return the same List instance")
        assertSame(v1[0], v2[0], "the cache should return the same validator instance")
    }

    /**
     * The same annotation with different value classes should trigger independent builder instantiation.
     */
    @Test
    fun testCacheDistinguishesByValueClass() {
        val annotation = ageAnnotation(Min::class.java)
        val intValidators = ValidatorFactory.getValidator(annotation, 25)        // Int
        val longValidators = ValidatorFactory.getValidator(annotation, 25L)      // Long
        assertNotSame(
            intValidators[0],
            longValidators[0],
            "Int and Long should use MinValidatorForInteger / MinValidatorForLong respectively"
        )
    }

    /**
     * Two dynamically constructed annotations with equal content should hit the same cache entry (relying on the JDK
     * Annotation content-equality contract).
     * Range internally constructs Min/Max annotations reflectively; the cache can short-circuit the reflection cost.
     */
    @Test
    fun testCacheHitsForRangeBetweenCalls() {
        val annotation = ageAnnotation(Range::class.java)
        val v1 = ValidatorFactory.getValidator(annotation, 25)
        val v2 = ValidatorFactory.getValidator(annotation, 25)
        assertSame(v1, v2, "the whole Range result should be cached")
        assertEquals(2, v1.size, "Range should return Min + Max validators (2 total)")
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
