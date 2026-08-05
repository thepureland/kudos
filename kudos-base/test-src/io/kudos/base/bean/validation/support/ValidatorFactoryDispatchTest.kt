package io.kudos.base.bean.validation.support

import io.kudos.base.bean.validation.constraint.annotations.AtLeast
import io.kudos.base.bean.validation.constraint.annotations.CnIdCardNo
import io.kudos.base.bean.validation.constraint.annotations.Compare
import io.kudos.base.bean.validation.constraint.annotations.Custom
import io.kudos.base.bean.validation.constraint.annotations.DateTime
import io.kudos.base.bean.validation.constraint.annotations.DictEnumItemCode
import io.kudos.base.bean.validation.constraint.annotations.Matches
import io.kudos.base.bean.validation.constraint.annotations.NotNullOn
import io.kudos.base.bean.validation.constraint.annotations.Series
import io.kudos.base.enums.impl.SexEnum
import jakarta.validation.ConstraintValidator
import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.Year
import java.time.YearMonth
import java.time.ZonedDateTime
import java.time.chrono.HijrahDate
import java.time.chrono.JapaneseDate
import java.time.chrono.MinguoDate
import java.time.chrono.ThaiBuddhistDate
import java.util.Calendar
import java.util.Date
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Exhaustive dispatch tests for [ValidatorFactory]: exercises every value-type branch of the
 * numericBound / dateBound / sizeBound templates, the Digits dispatch, every registered annotation
 * builder, and the unsupported-type error branches.
 *
 * @author K
 * @since 1.0.0
 */
internal class ValidatorFactoryDispatchTest {

    @BeforeTest
    fun clearCache() {
        ValidatorFactory.clearCacheForTest()
    }

    /** Obtain an annotation instance of [klass] declared on the getter of [property] in [Holder]. */
    private fun <A : Annotation> anno(property: String, klass: Class<A>): A =
        Holder::class.java.getDeclaredMethod("get" + property.replaceFirstChar { it.uppercase() })
            .getAnnotation(klass) ?: error("annotation $klass not found on $property")

    private fun assertOneValidator(list: List<ConstraintValidator<*, *>>) {
        assertEquals(1, list.size)
    }

    /** Every value type the numericBound template dispatches on. */
    private val numericValues: List<Any> = listOf(
        "1",                 // CharSequence
        1.0,                 // Double
        1,                   // Int
        1L,                  // Long
        1.0f,                // Float
        1.toByte(),          // Byte
        1.toShort(),         // Short
        BigDecimal.ONE,      // BigDecimal
        BigInteger.ONE,      // BigInteger
        AtomicNumber(1),     // generic Number
    )

    /** Every value type the dateBound template dispatches on. */
    private val dateValues: List<Any> = listOf(
        LocalDate.now(),
        LocalDateTime.now(),
        LocalTime.now(),
        Instant.now(),
        Calendar.getInstance(),
        Date(),
        HijrahDate.now(),
        JapaneseDate.now(),
        MinguoDate.now(),
        MonthDay.now(),
        OffsetDateTime.now(),
        OffsetTime.now(),
        ThaiBuddhistDate.now(),
        Year.now(),
        YearMonth.now(),
        ZonedDateTime.now(),
    )

    /** Every value type the sizeBound template dispatches on. */
    private val sizeValues: List<Any> = listOf(
        "ab",
        arrayOf("a", "b"),
        listOf("a", "b"),
        doubleArrayOf(1.0),
        intArrayOf(1),
        longArrayOf(1L),
        charArrayOf('a'),
        floatArrayOf(1.0f),
        booleanArrayOf(true),
        byteArrayOf(1),
        shortArrayOf(1),
        mapOf("a" to "b"),
    )

    // ---------------- numericBound: every annotation x every value type ----------------

    @Test
    fun numericBoundAllAnnotationsAllTypes() {
        val numericAnnotations = listOf(
            anno("num", Min::class.java),
            anno("num", Max::class.java),
            anno("num", DecimalMin::class.java),
            anno("num", DecimalMax::class.java),
            anno("num", Negative::class.java),
            anno("num", NegativeOrZero::class.java),
            anno("num", Positive::class.java),
            anno("num", PositiveOrZero::class.java),
        )
        for (a in numericAnnotations) {
            for (v in numericValues) {
                assertOneValidator(ValidatorFactory.getValidator(a, v))
            }
        }
    }

    @Test
    fun numericBoundUnsupportedTypeThrows() {
        val a = anno("num", Min::class.java)
        val ex = assertFailsWith<IllegalStateException> { ValidatorFactory.getValidator(a, Any()) }
        assertTrue(ex.message!!.contains("Min"))
    }

    // ---------------- Digits dispatch ----------------

    @Test
    fun digitsDispatch() {
        val a = anno("num", Digits::class.java)
        assertOneValidator(ValidatorFactory.getValidator(a, "12"))   // CharSequence
        assertOneValidator(ValidatorFactory.getValidator(a, 12))     // Number
        val ex = assertFailsWith<IllegalStateException> { ValidatorFactory.getValidator(a, Any()) }
        assertTrue(ex.message!!.contains("Digits"))
    }

    // ---------------- dateBound: every annotation x every value type ----------------

    @Test
    fun dateBoundAllAnnotationsAllTypes() {
        val dateAnnotations = listOf(
            anno("date", Future::class.java),
            anno("date", FutureOrPresent::class.java),
            anno("date", Past::class.java),
            anno("date", PastOrPresent::class.java),
        )
        for (a in dateAnnotations) {
            for (v in dateValues) {
                assertOneValidator(ValidatorFactory.getValidator(a, v))
            }
        }
    }

    @Test
    fun dateBoundUnsupportedTypeThrows() {
        val a = anno("date", Future::class.java)
        val ex = assertFailsWith<IllegalStateException> { ValidatorFactory.getValidator(a, Any()) }
        assertTrue(ex.message!!.contains("Future"))
    }

    // ---------------- sizeBound: every annotation x every value type ----------------

    @Test
    fun sizeBoundAllAnnotationsAllTypes() {
        val sizeAnnotations = listOf(
            anno("coll", Size::class.java),
            anno("coll", NotEmpty::class.java),
        )
        for (a in sizeAnnotations) {
            for (v in sizeValues) {
                assertOneValidator(ValidatorFactory.getValidator(a, v))
            }
        }
    }

    @Test
    fun sizeBoundUnsupportedTypeThrows() {
        val a = anno("coll", Size::class.java)
        val ex = assertFailsWith<IllegalStateException> { ValidatorFactory.getValidator(a, 1) }
        assertTrue(ex.message!!.contains("Size"))
    }

    @Test
    fun notEmptyUnsupportedTypeThrows() {
        val a = anno("coll", NotEmpty::class.java)
        val ex = assertFailsWith<IllegalStateException> { ValidatorFactory.getValidator(a, 1) }
        assertTrue(ex.message!!.contains("NotEmpty"))
    }

    // ---------------- simple / raw single-validator annotations ----------------

    @Test
    fun simpleAndRawAnnotations() {
        assertOneValidator(ValidatorFactory.getValidator(anno("bool", AssertFalse::class.java), false))
        assertOneValidator(ValidatorFactory.getValidator(anno("bool", AssertTrue::class.java), true))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", NotBlank::class.java), "a"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", NotNull::class.java), "a"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Null::class.java), "a"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Email::class.java), "a@b.com"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Pattern::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("coll", UniqueElements::class.java), listOf("a")))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", URL::class.java), "http://a.com"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", CodePointLength::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", EAN::class.java), "1234567890123"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", ISBN::class.java), "1234567890"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Length::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", LuhnCheck::class.java), "12"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Mod10Check::class.java), "12"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Mod11Check::class.java), "12"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", ParameterScriptAssert::class.java), "x"))
    }

    @Test
    fun cacheHitInvokesCacheKeyEquals() {
        // First call populates the cache (key.hashCode); the second call hits it, exercising CacheKey.equals.
        val a = anno("num", Min::class.java)
        val first = ValidatorFactory.getValidator(a, 1)
        val second = ValidatorFactory.getValidator(a, 1)
        kotlin.test.assertSame(first, second)
    }

    // ---------------- composite annotations ----------------

    @Test
    fun rangeReturnsMinAndMax() {
        val validators = ValidatorFactory.getValidator(anno("num", Range::class.java), 5)
        assertEquals(2, validators.size)
    }

    @Test
    fun creditCardNumberDelegatesToLuhn() {
        val validators = ValidatorFactory.getValidator(anno("str", CreditCardNumber::class.java), "4111111111111111")
        assertOneValidator(validators)
    }

    // ---------------- kudos custom annotations ----------------

    @Test
    fun kudosCharSequenceAnnotations() {
        assertOneValidator(ValidatorFactory.getValidator(anno("str", CnIdCardNo::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", DateTime::class.java), "2026-01-01"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", DictEnumItemCode::class.java), "1"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Matches::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Compare::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", Custom::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("str", NotNullOn::class.java), "abc"))
        assertOneValidator(ValidatorFactory.getValidator(anno("coll", Series::class.java), listOf(1, 2, 3)))
    }

    @Test
    fun atLeastFromClassAnnotation() {
        val a = AtLeastHolder::class.java.getAnnotation(AtLeast::class.java)
        assertOneValidator(ValidatorFactory.getValidator(a, AtLeastHolder()))
    }

    @Test
    fun matchesUnsupportedTypeThrows() {
        val a = anno("str", Matches::class.java)
        val ex = assertFailsWith<IllegalStateException> { ValidatorFactory.getValidator(a, 1) }
        assertTrue(ex.message!!.contains("Matches"))
    }

    // ---------------- unsupported annotation -> empty ----------------

    @Test
    fun unsupportedAnnotationReturnsEmpty() {
        val a = anno("supportedMarker", SupportedMarker::class.java)
        assertTrue(ValidatorFactory.getValidator(a, "x").isEmpty())
    }

    /** A custom non-Number-subclass-by-name yet Number subclass to hit the generic `number` branch. */
    private class AtomicNumber(private val v: Int) : Number() {
        override fun toByte() = v.toByte()
        override fun toDouble() = v.toDouble()
        override fun toFloat() = v.toFloat()
        override fun toInt() = v
        override fun toLong() = v.toLong()
        override fun toShort() = v.toShort()
    }

    /** A non-constraint annotation, used to verify the empty-list path. */
    @Retention(AnnotationRetention.RUNTIME)
    @Target(AnnotationTarget.PROPERTY_GETTER, AnnotationTarget.FIELD)
    annotation class SupportedMarker

    @Suppress("unused")
    private class Holder {
        @get:Min(1)
        @get:Max(10)
        @get:DecimalMin("1")
        @get:DecimalMax("10")
        @get:Negative
        @get:NegativeOrZero
        @get:Positive
        @get:PositiveOrZero
        @get:Digits(integer = 3, fraction = 2)
        @get:Range(min = 1, max = 10)
        val num: Int = 1

        @get:Future
        @get:FutureOrPresent
        @get:Past
        @get:PastOrPresent
        val date: LocalDate? = null

        @get:Size(min = 0, max = 100)
        @get:NotEmpty
        @get:UniqueElements
        @get:Series
        val coll: List<String> = listOf()

        @get:AssertFalse
        @get:AssertTrue
        val bool: Boolean = false

        @get:NotBlank
        @get:NotNull
        @get:Null
        @get:Email
        @get:Pattern(regexp = ".*")
        @get:URL
        @get:CodePointLength(min = 0, max = 100)
        @get:EAN
        @get:ISBN
        @get:Length(min = 0, max = 100)
        @get:LuhnCheck
        @get:Mod10Check
        @get:Mod11Check
        @get:ParameterScriptAssert(lang = "groovy", script = "true")
        @get:CreditCardNumber
        @get:CnIdCardNo
        @get:DateTime(format = "yyyy-MM-dd")
        @get:DictEnumItemCode(enumClass = SexEnum::class)
        @get:Matches(io.kudos.base.bean.validation.support.RegExpEnum.VAR_NAME)
        @get:Compare(anotherProperty = "other")
        @get:Custom(checkClass = NoopBeanValidator::class)
        @get:NotNullOn(depends = io.kudos.base.bean.validation.support.Depends(properties = ["other"], values = ["x"]))
        val str: String? = null

        @get:SupportedMarker
        val supportedMarker: String? = null
    }

    /** No-op bean validator used as the checkClass for the @Custom annotation. */
    class NoopBeanValidator : io.kudos.base.bean.validation.support.IBeanValidator<Any?> {
        override fun validate(bean: Any?): Boolean = true
    }

    @AtLeast(properties = ["a", "b"])
    private class AtLeastHolder
}
