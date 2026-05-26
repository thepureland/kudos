package io.kudos.base.bean.validation.support

import io.kudos.base.bean.validation.constraint.annotations.*
import io.kudos.base.bean.validation.constraint.validator.*
import jakarta.validation.ConstraintValidator
import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.*
import org.hibernate.validator.internal.constraintvalidators.bv.*
import org.hibernate.validator.internal.constraintvalidators.bv.notempty.*
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.*
import org.hibernate.validator.internal.constraintvalidators.bv.number.bound.decimal.*
import org.hibernate.validator.internal.constraintvalidators.bv.number.sign.*
import org.hibernate.validator.internal.constraintvalidators.bv.size.*
import org.hibernate.validator.internal.constraintvalidators.bv.time.future.*
import org.hibernate.validator.internal.constraintvalidators.bv.time.futureorpresent.*
import org.hibernate.validator.internal.constraintvalidators.bv.time.past.*
import org.hibernate.validator.internal.constraintvalidators.bv.time.pastorpresent.*
import org.hibernate.validator.internal.constraintvalidators.hv.*
import java.math.BigDecimal
import java.math.BigInteger
import java.time.*
import java.time.chrono.HijrahDate
import java.time.chrono.JapaneseDate
import java.time.chrono.MinguoDate
import java.time.chrono.ThaiBuddhistDate
import java.util.Calendar
import java.util.Date
import java.util.concurrent.ConcurrentHashMap
import kotlin.reflect.KClass

private typealias ValidatorBuilder = (annotation: Annotation, value: Any) -> List<ConstraintValidator<*, *>>

/**
 * Validator factory.
 *
 * Looks up the corresponding ConstraintValidator by annotation type. Internally based on a [BUILDERS] registry:
 * - Simple annotations (no dispatch by value type) are listed directly in the table, one per line
 * - The numeric / date / Size groups share the same value-type-dispatched factory functions
 *   ([numericBound] / [dateBound] / [sizeBound]) to avoid duplicating the same when-branches in every annotation
 * - Composite annotations (e.g., Range = Min + Max, CreditCardNumber -> LuhnCheck) use inline lambdas
 *
 * To add a new annotation, simply add an entry to [BUILDERS] -- no need to modify a 350-line when block.
 *
 * ## Instance caching
 *
 * The returned validator list is cached by (annotation, value's runtime class), relying on the JDK Annotation
 * content-equality contract: two `@Min(10)` instances with the same attributes hit the same cache key.
 *
 * This eliminates repeated `new XxxValidator()` calls and ValidatorFactory-level `initialize` invocations.
 * The savings are especially significant for composite annotations like Range / CreditCardNumber that
 * reflectively construct inner annotations (short-circuiting [createAnnotationByNamedArgs]).
 * Note that the caller ConstraintsValidator will still call initialize again (using the HV descriptor + initCtx overload);
 * the cache does not affect its semantics.
 *
 * @author K
 * @since 1.0.0
 */
object ValidatorFactory {

    /** Validator instance cache: keyed by (annotation, value's runtime class) to avoid duplicate construction and initialize */
    private val CACHE: MutableMap<CacheKey, List<ConstraintValidator<*, *>>> = ConcurrentHashMap()

    /**
     * Return the validator instances corresponding to the validation rule annotation.
     *
     * @param annotation the validation rule annotation
     * @param value the value to validate
     * @return the list of validators; returns an empty list for unsupported annotations (composite annotations like Range may return multiple)
     */
    fun getValidator(annotation: Annotation, value: Any): List<ConstraintValidator<*, *>> {
        val key = CacheKey(annotation, value::class.java)
        return CACHE.computeIfAbsent(key) { build(annotation, value) }
    }

    /**
     * For testing only: clear the cache. Production code must not depend on this method.
     */
    internal fun clearCacheForTest() {
        CACHE.clear()
    }

    /**
     * The actual construction path on cache miss: look up the factory closure for the annotation in the [BUILDERS] registry and execute it.
     *
     * @param annotation the current annotation
     * @param value the value being validated (used for runtime-type dispatch)
     * @return the list of validators for the annotation; returns an empty list for unregistered annotations
     * @author K
     * @since 1.0.0
     */
    private fun build(annotation: Annotation, value: Any): List<ConstraintValidator<*, *>> {
        val builder = BUILDERS[annotation.annotationClass] ?: return emptyList()
        return builder(annotation, value)
    }

    /**
     * Composite key for the validator cache.
     * The annotation itself participates in hashCode/equals via the JDK Annotation "same attributes implies equal" contract.
     *
     * @property annotation the current annotation instance
     * @property valueClass the runtime class of the value being validated, so validators dispatched by different types are cached separately
     */
    private data class CacheKey(val annotation: Annotation, val valueClass: Class<*>)

    // ----------------------------- helpers -----------------------------

    /**
     * Erase the generic type parameters of ConstraintValidator, uniformly invoke [ConstraintValidator.initialize] and return itself,
     * for chained-style list construction inside factory closures.
     *
     * @param validator the already instantiated validator
     * @param annotation the annotation used to initialize
     * @return the same validator instance
     * @author K
     * @since 1.0.0
     */
    private fun initialize(
        validator: ConstraintValidator<*, *>,
        annotation: Annotation
    ): ConstraintValidator<*, *> {
        @Suppress("UNCHECKED_CAST")
        (validator as ConstraintValidator<Annotation, *>).initialize(annotation)
        return validator
    }

    /** Single validator that does not depend on the value type and needs no initialize */
    private fun raw(create: () -> ConstraintValidator<*, *>): ValidatorBuilder =
        { _, _ -> listOf(create()) }

    /** Single validator that does not depend on the value type but needs initialize */
    private fun simple(create: () -> ConstraintValidator<*, *>): ValidatorBuilder =
        { annotation, _ -> listOf(initialize(create(), annotation)) }

    /**
     * Dispatch template for numeric constraints (DecimalMax / DecimalMin / Max / Min / Negative / NegativeOrZero / Positive / PositiveOrZero).
     * Selects the corresponding built-in validator based on the actual type of the value and uniformly calls initialize(annotation).
     */
    private fun numericBound(
        name: String,
        charSeq: () -> ConstraintValidator<*, *>,
        double: () -> ConstraintValidator<*, *>,
        int: () -> ConstraintValidator<*, *>,
        long: () -> ConstraintValidator<*, *>,
        float: () -> ConstraintValidator<*, *>,
        byte: () -> ConstraintValidator<*, *>,
        short: () -> ConstraintValidator<*, *>,
        bigDecimal: () -> ConstraintValidator<*, *>,
        bigInteger: () -> ConstraintValidator<*, *>,
        number: () -> ConstraintValidator<*, *>
    ): ValidatorBuilder = { annotation, value ->
        val factory = when (value) {
            is CharSequence -> charSeq
            is Double -> double
            is Int -> int
            is Long -> long
            is Float -> float
            is Byte -> byte
            is Short -> short
            is BigDecimal -> bigDecimal
            is BigInteger -> bigInteger
            is Number -> number
            else -> error("The ${name} constraint annotation does not support validation of type [${value::class}]!")
        }
        listOf(initialize(factory(), annotation))
    }

    /** Dispatch template for date/time constraints (Future / FutureOrPresent / Past / PastOrPresent). */
    private fun dateBound(
        name: String,
        localDate: () -> ConstraintValidator<*, *>,
        localDateTime: () -> ConstraintValidator<*, *>,
        localTime: () -> ConstraintValidator<*, *>,
        instant: () -> ConstraintValidator<*, *>,
        calendar: () -> ConstraintValidator<*, *>,
        date: () -> ConstraintValidator<*, *>,
        hijrahDate: () -> ConstraintValidator<*, *>,
        japaneseDate: () -> ConstraintValidator<*, *>,
        minguoDate: () -> ConstraintValidator<*, *>,
        monthDay: () -> ConstraintValidator<*, *>,
        offsetDateTime: () -> ConstraintValidator<*, *>,
        offsetTime: () -> ConstraintValidator<*, *>,
        thaiBuddhistDate: () -> ConstraintValidator<*, *>,
        year: () -> ConstraintValidator<*, *>,
        yearMonth: () -> ConstraintValidator<*, *>,
        zonedDateTime: () -> ConstraintValidator<*, *>
    ): ValidatorBuilder = { annotation, value ->
        val factory = when (value) {
            is LocalDate -> localDate
            is LocalDateTime -> localDateTime
            is LocalTime -> localTime
            is Instant -> instant
            is Calendar -> calendar
            is Date -> date
            is HijrahDate -> hijrahDate
            is JapaneseDate -> japaneseDate
            is MinguoDate -> minguoDate
            is MonthDay -> monthDay
            is OffsetDateTime -> offsetDateTime
            is OffsetTime -> offsetTime
            is ThaiBuddhistDate -> thaiBuddhistDate
            is Year -> year
            is YearMonth -> yearMonth
            is ZonedDateTime -> zonedDateTime
            else -> error("The ${name} constraint annotation does not support validation of type [${value::class}]!")
        }
        listOf(initialize(factory(), annotation))
    }

    /** Dispatch template for collection/array constraints (NotEmpty / Size). */
    private fun sizeBound(
        name: String,
        charSeq: () -> ConstraintValidator<*, *>,
        array: () -> ConstraintValidator<*, *>,
        collection: () -> ConstraintValidator<*, *>,
        doubleArray: () -> ConstraintValidator<*, *>,
        intArray: () -> ConstraintValidator<*, *>,
        longArray: () -> ConstraintValidator<*, *>,
        charArray: () -> ConstraintValidator<*, *>,
        floatArray: () -> ConstraintValidator<*, *>,
        booleanArray: () -> ConstraintValidator<*, *>,
        byteArray: () -> ConstraintValidator<*, *>,
        shortArray: () -> ConstraintValidator<*, *>,
        map: () -> ConstraintValidator<*, *>
    ): ValidatorBuilder = { annotation, value ->
        val factory = when (value) {
            is CharSequence -> charSeq
            is Array<*> -> array
            is Collection<*> -> collection
            is DoubleArray -> doubleArray
            is IntArray -> intArray
            is LongArray -> longArray
            is CharArray -> charArray
            is FloatArray -> floatArray
            is BooleanArray -> booleanArray
            is ByteArray -> byteArray
            is ShortArray -> shortArray
            is Map<*, *> -> map
            else -> error("The ${name} constraint annotation does not support validation of type [${value::class}]!")
        }
        listOf(initialize(factory(), annotation))
    }

    /**
     * Reflectively construct an annotation instance: match [namedArgs] by formal parameter name; use defaults for any parameters not matched.
     * Used to split composite constraints (Range, CreditCardNumber) into smaller inner constraints (Min/Max, LuhnCheck) and dispatch to the corresponding validators.
     *
     * @param A the target annotation type
     * @param annotationClass the KClass of the target annotation
     * @param namedArgs mapping from formal parameter name to argument value
     * @return the instantiated annotation
     * @throws IllegalStateException when the annotation constructor cannot be found
     * @author K
     * @since 1.0.0
     */
    private fun <A : Annotation> createAnnotationByNamedArgs(
        annotationClass: KClass<A>,
        namedArgs: Map<String, Any>
    ): A {
        val constructor = annotationClass.constructors.firstOrNull()
            ?: error("Cannot find a constructor for annotation [${annotationClass.qualifiedName}]")
        val callArgs = constructor.parameters
            .mapNotNull { parameter ->
                val name = parameter.name
                if (name != null && namedArgs.containsKey(name)) {
                    parameter to namedArgs.getValue(name)
                } else {
                    null
                }
            }
            .toMap()
        return constructor.callBy(callArgs)
    }

    // ----------------------------- registry -----------------------------

    /**
     * Registry from annotation types to validator construction closures.
     * To add a new annotation, simply append an entry here; annotations dispatched by value type go through the
     * [numericBound] / [dateBound] / [sizeBound] templates, while composite annotations (Range = Min+Max,
     * CreditCardNumber -> LuhnCheck) are decomposed via inline lambdas.
     */
    private val BUILDERS: Map<KClass<out Annotation>, ValidatorBuilder> = buildMap {
        // ---- jakarta.validation ----
        this[AssertFalse::class] = raw { AssertFalseValidator() }
        this[AssertTrue::class] = raw { AssertTrueValidator() }
        this[NotBlank::class] = raw { NotBlankValidator() }
        this[NotNull::class] = raw { NotNullValidator() }
        this[Null::class] = raw { NullValidator() }
        this[Email::class] = simple { EmailValidator() }
        this[Pattern::class] = simple { PatternValidator() }

        this[DecimalMax::class] = numericBound(
            name = "DecimalMax",
            charSeq = { DecimalMaxValidatorForCharSequence() },
            double = { DecimalMaxValidatorForDouble() },
            int = { DecimalMaxValidatorForInteger() },
            long = { DecimalMaxValidatorForLong() },
            float = { DecimalMaxValidatorForFloat() },
            byte = { DecimalMaxValidatorForByte() },
            short = { DecimalMaxValidatorForShort() },
            bigDecimal = { DecimalMaxValidatorForBigDecimal() },
            bigInteger = { DecimalMaxValidatorForBigInteger() },
            number = { DecimalMaxValidatorForNumber() },
        )
        this[DecimalMin::class] = numericBound(
            name = "DecimalMin",
            charSeq = { DecimalMinValidatorForCharSequence() },
            double = { DecimalMinValidatorForDouble() },
            int = { DecimalMinValidatorForInteger() },
            long = { DecimalMinValidatorForLong() },
            float = { DecimalMinValidatorForFloat() },
            byte = { DecimalMinValidatorForByte() },
            short = { DecimalMinValidatorForShort() },
            bigDecimal = { DecimalMinValidatorForBigDecimal() },
            bigInteger = { DecimalMinValidatorForBigInteger() },
            number = { DecimalMinValidatorForNumber() },
        )
        this[Max::class] = numericBound(
            name = "Max",
            charSeq = { MaxValidatorForCharSequence() },
            double = { MaxValidatorForDouble() },
            int = { MaxValidatorForInteger() },
            long = { MaxValidatorForLong() },
            float = { MaxValidatorForFloat() },
            byte = { MaxValidatorForByte() },
            short = { MaxValidatorForShort() },
            bigDecimal = { MaxValidatorForBigDecimal() },
            bigInteger = { MaxValidatorForBigInteger() },
            number = { MaxValidatorForNumber() },
        )
        this[Min::class] = numericBound(
            name = "Min",
            charSeq = { MinValidatorForCharSequence() },
            double = { MinValidatorForDouble() },
            int = { MinValidatorForInteger() },
            long = { MinValidatorForLong() },
            float = { MinValidatorForFloat() },
            byte = { MinValidatorForByte() },
            short = { MinValidatorForShort() },
            bigDecimal = { MinValidatorForBigDecimal() },
            bigInteger = { MinValidatorForBigInteger() },
            number = { MinValidatorForNumber() },
        )
        this[Negative::class] = numericBound(
            name = "Negative",
            charSeq = { NegativeValidatorForCharSequence() },
            double = { NegativeValidatorForDouble() },
            int = { NegativeValidatorForInteger() },
            long = { NegativeValidatorForLong() },
            float = { NegativeValidatorForFloat() },
            byte = { NegativeValidatorForByte() },
            short = { NegativeValidatorForShort() },
            bigDecimal = { NegativeValidatorForBigDecimal() },
            bigInteger = { NegativeValidatorForBigInteger() },
            number = { NegativeValidatorForNumber() },
        )
        this[NegativeOrZero::class] = numericBound(
            name = "NegativeOrZero",
            charSeq = { NegativeOrZeroValidatorForCharSequence() },
            double = { NegativeOrZeroValidatorForDouble() },
            int = { NegativeOrZeroValidatorForInteger() },
            long = { NegativeOrZeroValidatorForLong() },
            float = { NegativeOrZeroValidatorForFloat() },
            byte = { NegativeOrZeroValidatorForByte() },
            short = { NegativeOrZeroValidatorForShort() },
            bigDecimal = { NegativeOrZeroValidatorForBigDecimal() },
            bigInteger = { NegativeOrZeroValidatorForBigInteger() },
            number = { NegativeOrZeroValidatorForNumber() },
        )
        this[Positive::class] = numericBound(
            name = "Positive",
            charSeq = { PositiveValidatorForCharSequence() },
            double = { PositiveValidatorForDouble() },
            int = { PositiveValidatorForInteger() },
            long = { PositiveValidatorForLong() },
            float = { PositiveValidatorForFloat() },
            byte = { PositiveValidatorForByte() },
            short = { PositiveValidatorForShort() },
            bigDecimal = { PositiveValidatorForBigDecimal() },
            bigInteger = { PositiveValidatorForBigInteger() },
            number = { PositiveValidatorForNumber() },
        )
        this[PositiveOrZero::class] = numericBound(
            name = "PositiveOrZero",
            charSeq = { PositiveOrZeroValidatorForCharSequence() },
            double = { PositiveOrZeroValidatorForDouble() },
            int = { PositiveOrZeroValidatorForInteger() },
            long = { PositiveOrZeroValidatorForLong() },
            float = { PositiveOrZeroValidatorForFloat() },
            byte = { PositiveOrZeroValidatorForByte() },
            short = { PositiveOrZeroValidatorForShort() },
            bigDecimal = { PositiveOrZeroValidatorForBigDecimal() },
            bigInteger = { PositiveOrZeroValidatorForBigInteger() },
            number = { PositiveOrZeroValidatorForNumber() },
        )

        this[Digits::class] = { annotation, value ->
            val factory: () -> ConstraintValidator<*, *> = when (value) {
                is CharSequence -> { -> DigitsValidatorForCharSequence() }
                is Number -> { -> DigitsValidatorForNumber() }
                else -> error("The Digits constraint annotation does not support validation of type [${value::class}]!")
            }
            listOf(initialize(factory(), annotation))
        }

        this[Future::class] = dateBound(
            name = "Future",
            localDate = { FutureValidatorForLocalDate() },
            localDateTime = { FutureValidatorForLocalDateTime() },
            localTime = { FutureValidatorForLocalTime() },
            instant = { FutureValidatorForInstant() },
            calendar = { FutureValidatorForCalendar() },
            date = { FutureValidatorForDate() },
            hijrahDate = { FutureValidatorForHijrahDate() },
            japaneseDate = { FutureValidatorForJapaneseDate() },
            minguoDate = { FutureValidatorForMinguoDate() },
            monthDay = { FutureValidatorForMonthDay() },
            offsetDateTime = { FutureValidatorForOffsetDateTime() },
            offsetTime = { FutureValidatorForOffsetTime() },
            thaiBuddhistDate = { FutureValidatorForThaiBuddhistDate() },
            year = { FutureValidatorForYear() },
            yearMonth = { FutureValidatorForYearMonth() },
            zonedDateTime = { FutureValidatorForZonedDateTime() },
        )
        this[FutureOrPresent::class] = dateBound(
            name = "FutureOrPresent",
            localDate = { FutureOrPresentValidatorForLocalDate() },
            localDateTime = { FutureOrPresentValidatorForLocalDateTime() },
            localTime = { FutureOrPresentValidatorForLocalTime() },
            instant = { FutureOrPresentValidatorForInstant() },
            calendar = { FutureOrPresentValidatorForCalendar() },
            date = { FutureOrPresentValidatorForDate() },
            hijrahDate = { FutureOrPresentValidatorForHijrahDate() },
            japaneseDate = { FutureOrPresentValidatorForJapaneseDate() },
            minguoDate = { FutureOrPresentValidatorForMinguoDate() },
            monthDay = { FutureOrPresentValidatorForMonthDay() },
            offsetDateTime = { FutureOrPresentValidatorForOffsetDateTime() },
            offsetTime = { FutureOrPresentValidatorForOffsetTime() },
            thaiBuddhistDate = { FutureOrPresentValidatorForThaiBuddhistDate() },
            year = { FutureOrPresentValidatorForYear() },
            yearMonth = { FutureOrPresentValidatorForYearMonth() },
            zonedDateTime = { FutureOrPresentValidatorForZonedDateTime() },
        )
        this[Past::class] = dateBound(
            name = "Past",
            localDate = { PastValidatorForLocalDate() },
            localDateTime = { PastValidatorForLocalDateTime() },
            localTime = { PastValidatorForLocalTime() },
            instant = { PastValidatorForInstant() },
            calendar = { PastValidatorForCalendar() },
            date = { PastValidatorForDate() },
            hijrahDate = { PastValidatorForHijrahDate() },
            japaneseDate = { PastValidatorForJapaneseDate() },
            minguoDate = { PastValidatorForMinguoDate() },
            monthDay = { PastValidatorForMonthDay() },
            offsetDateTime = { PastValidatorForOffsetDateTime() },
            offsetTime = { PastValidatorForOffsetTime() },
            thaiBuddhistDate = { PastValidatorForThaiBuddhistDate() },
            year = { PastValidatorForYear() },
            yearMonth = { PastValidatorForYearMonth() },
            zonedDateTime = { PastValidatorForZonedDateTime() },
        )
        this[PastOrPresent::class] = dateBound(
            name = "PastOrPresent",
            localDate = { PastOrPresentValidatorForLocalDate() },
            localDateTime = { PastOrPresentValidatorForLocalDateTime() },
            localTime = { PastOrPresentValidatorForLocalTime() },
            instant = { PastOrPresentValidatorForInstant() },
            calendar = { PastOrPresentValidatorForCalendar() },
            date = { PastOrPresentValidatorForDate() },
            hijrahDate = { PastOrPresentValidatorForHijrahDate() },
            japaneseDate = { PastOrPresentValidatorForJapaneseDate() },
            minguoDate = { PastOrPresentValidatorForMinguoDate() },
            monthDay = { PastOrPresentValidatorForMonthDay() },
            offsetDateTime = { PastOrPresentValidatorForOffsetDateTime() },
            offsetTime = { PastOrPresentValidatorForOffsetTime() },
            thaiBuddhistDate = { PastOrPresentValidatorForThaiBuddhistDate() },
            year = { PastOrPresentValidatorForYear() },
            yearMonth = { PastOrPresentValidatorForYearMonth() },
            zonedDateTime = { PastOrPresentValidatorForZonedDateTime() },
        )

        this[NotEmpty::class] = sizeBound(
            name = "NotEmpty",
            charSeq = { NotEmptyValidatorForCharSequence() },
            array = { NotEmptyValidatorForArray() },
            collection = { NotEmptyValidatorForCollection() },
            doubleArray = { NotEmptyValidatorForArraysOfDouble() },
            intArray = { NotEmptyValidatorForArraysOfInt() },
            longArray = { NotEmptyValidatorForArraysOfLong() },
            charArray = { NotEmptyValidatorForArraysOfChar() },
            floatArray = { NotEmptyValidatorForArraysOfFloat() },
            booleanArray = { NotEmptyValidatorForArraysOfBoolean() },
            byteArray = { NotEmptyValidatorForArraysOfByte() },
            shortArray = { NotEmptyValidatorForArraysOfShort() },
            map = { NotEmptyValidatorForMap() },
        )
        this[Size::class] = sizeBound(
            name = "Size",
            charSeq = { SizeValidatorForCharSequence() },
            array = { SizeValidatorForArray() },
            collection = { SizeValidatorForCollection() },
            doubleArray = { SizeValidatorForArraysOfDouble() },
            intArray = { SizeValidatorForArraysOfInt() },
            longArray = { SizeValidatorForArraysOfLong() },
            charArray = { SizeValidatorForArraysOfChar() },
            floatArray = { SizeValidatorForArraysOfFloat() },
            booleanArray = { SizeValidatorForArraysOfBoolean() },
            byteArray = { SizeValidatorForArraysOfByte() },
            shortArray = { SizeValidatorForArraysOfShort() },
            map = { SizeValidatorForMap() },
        )

        // ---- hibernate ----
        this[CodePointLength::class] = simple { CodePointLengthValidator() }
        this[CreditCardNumber::class] = { annotation, value ->
            val cc = annotation as CreditCardNumber
            val luhnCheck = createAnnotationByNamedArgs(
                LuhnCheck::class,
                mapOf("ignoreNonDigitCharacters" to cc.ignoreNonDigitCharacters)
            )
            getValidator(luhnCheck, value)
        }
        this[EAN::class] = simple { EANValidator() }
        this[ISBN::class] = simple { ISBNValidator() }
        this[Length::class] = simple { LengthValidator() }
        this[LuhnCheck::class] = simple { LuhnCheckValidator() }
        this[Mod10Check::class] = simple { Mod10CheckValidator() }
        this[Mod11Check::class] = simple { Mod11CheckValidator() }
        this[ParameterScriptAssert::class] = simple { ParameterScriptAssertValidator() }
        this[Range::class] = { annotation, value ->
            val range = annotation as Range
            val minAnnotation = createAnnotationByNamedArgs(Min::class, mapOf("value" to range.min))
            val maxAnnotation = createAnnotationByNamedArgs(Max::class, mapOf("value" to range.max))
            listOf(
                getValidator(minAnnotation, value).first(),
                getValidator(maxAnnotation, value).first()
            )
        }
        this[UniqueElements::class] = raw { UniqueElementsValidator() }
        this[URL::class] = simple { URLValidator() }

        // ---- kudos ----
        this[AtLeast::class] = simple { AtLeastValidator() }
        this[CnIdCardNo::class] = simple { CnIdCardNoValidator() }
        this[Compare::class] = simple { CompareValidator() }
        this[Custom::class] = simple { CustomValidator() }
        this[DateTime::class] = simple { DateTimeValidator() }
        this[DictEnumItemCode::class] = simple { DictEnumCodeValidator() }
        this[NotNullOn::class] = simple { NotNullOnValidator() }
        this[Series::class] = simple { SeriesValidator() }
        this[Matches::class] = { annotation, value ->
            if (value !is CharSequence) {
                error("The Matches constraint annotation does not support validation of type [${value::class}]!")
            }
            listOf(initialize(MatchesValidator(), annotation))
        }
    }

}
