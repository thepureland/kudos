package io.kudos.base.bean.validation.constraint.annotations

import io.kudos.base.bean.validation.constraint.validator.ConstraintsValidator
import io.kudos.base.bean.validation.support.Depends
import io.kudos.base.bean.validation.support.RegExpEnum
import io.kudos.base.bean.validation.support.IBeanValidator
import io.kudos.base.enums.ienums.IDictEnum
import io.kudos.base.support.logic.AndOrEnum
import jakarta.validation.Constraint
import jakarta.validation.Payload
import jakarta.validation.constraints.*
import org.hibernate.validator.constraints.*
import kotlin.reflect.KClass

/**
 * Composite constraint annotation. It can serve the following purposes:
 * 1. Validate constraints in the declared order (a replacement for @GroupSequence and @GroupSequenceProvider).
 * 2. Allow validation to pass as long as one of the constraints passes (AndOr.Or).
 *
 * Usage limitations:
 * 1. The List specification inside constraint annotations is not yet supported.
 *
 * @author K
 * @since 1.0.0
 */
@Constraint(validatedBy = [ConstraintsValidator::class])
@Target(
    AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY_GETTER,
    AnnotationTarget.PROPERTY_SETTER
)
@Retention(AnnotationRetention.RUNTIME)
@MustBeDocumented
@Repeatable
annotation class Constraints(

    /** Validation order of the sub-constraints. */
    val order: Array<KClass<out Annotation>> = [],

    /** Logical relationship between the constraints; when AND, all constraints must pass to pass overall; when OR, validation passes as long as any one constraint passes. */
    val andOr: AndOrEnum = AndOrEnum.AND,

    // Constraints defined by jakarta.validation
    /** Logical-false constraint; the validated object must be a Boolean and its value must be false. */
    val assertFalse: AssertFalse = AssertFalse(message = MESSAGE),
    /** Logical-true constraint; the validated object must be a Boolean and its value must be true. */
    val assertTrue: AssertTrue = AssertTrue(message = MESSAGE),
    /**
     * Maximum-value constraint, defined via a String value; inclusive or exclusive can be specified.
     * The validated object's type must be one of the following or a subclass: CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount.
     */
    val decimalMax: DecimalMax = DecimalMax("", message = MESSAGE),
    /**
     * Minimum-value constraint, defined via a String value; inclusive or exclusive can be specified.
     * The validated object's type must be one of the following or a subclass: CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount.
     */
    val decimalMin: DecimalMin = DecimalMin("", message = MESSAGE),
    /** Numeric constraint; the integer and fractional digit counts can be specified separately. The validated object's type must be one of the following or a subclass: CharSequence, Number, MonetaryAmount. */
    val digits: Digits = Digits(integer = 0, fraction = 0, message = MESSAGE),
    /** Email constraint; the validated object's type must be CharSequence or a subclass. Unless there is something special, regexp need not be specified. */
    val email: Email = Email(message = MESSAGE),
    /**
     * Future date-time constraint; the validated object's type must be one of the following or a subclass: LocalDate, LocalDateTime, LocalTime, Instant, Calendar, Date, HijrahDate,
     * JapaneseDate, MinguoDate, MonthDay, OffsetDateTime, OffsetTime, ThaiBuddhistDate, Year, YearMonth, ZonedDateTime.
     */
    val future: Future = Future(message = MESSAGE),
    /**
     * Future-or-present date-time constraint; the validated object's type must be one of the following or a subclass: LocalDate, LocalDateTime, LocalTime, Instant, Calendar, Date, HijrahDate,
     * JapaneseDate, MinguoDate, MonthDay, OffsetDateTime, OffsetTime, ThaiBuddhistDate, Year, YearMonth, ZonedDateTime.
     */
    val futureOrPresent: FutureOrPresent = FutureOrPresent(message = MESSAGE),
    /**
     * Maximum-value constraint, defined via a long value. The validated object's type must be one of the following or a subclass:
     * CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount.
     */
    val max: Max = Max(0, message = MESSAGE),
    /**
     * Minimum-value constraint, defined via a long value. The validated object's type must be one of the following or a subclass:
     * CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount.
     */
    val min: Min = Min(0, message = MESSAGE),
    /** Negative-number constraint; the validated object's type must be one of the following or a subclass: CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount. */
    val negative: Negative = Negative(message = MESSAGE),
    /** Negative-or-zero constraint; the validated object's type must be one of the following or a subclass: CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount. */
    val negativeOrZero: NegativeOrZero = NegativeOrZero(message = MESSAGE),
    /** Not-blank constraint; the validated object's type must be CharSequence or a subclass. */
    val notBlank: NotBlank = NotBlank(message = MESSAGE),
    /**
     * Not-empty constraint; the validated object's type must be one of the following or a subclass:
     * CharSequence, Array<*>, Collection<*>, DoubleArray, IntArray, LongArray, CharArray, FloatArray, BooleanArray, ByteArray, ShortArray, Map<*, *>.
     */
    val notEmpty: NotEmpty = NotEmpty(message = MESSAGE),
    /** Not-null constraint; the validated object may be of any type. */
    val notNull: NotNull = NotNull(message = MESSAGE),
    /** Null constraint; the validated object may be of any type. */
    val beNull: Null = Null(message = MESSAGE),
    /**
     * Past date-time constraint; the validated object's type must be one of the following or a subclass: LocalDate, LocalDateTime, LocalTime, Instant, Calendar, Date, HijrahDate,
     * JapaneseDate, MinguoDate, MonthDay, OffsetDateTime, OffsetTime, ThaiBuddhistDate, Year, YearMonth, ZonedDateTime.
     */
    val past: Past = Past(message = MESSAGE),
    /**
     * Past-or-present date-time constraint; the validated object's type must be one of the following or a subclass: LocalDate, LocalDateTime, LocalTime, Instant, Calendar, Date, HijrahDate,
     * JapaneseDate, MinguoDate, MonthDay, OffsetDateTime, OffsetTime, ThaiBuddhistDate, Year, YearMonth, ZonedDateTime.
     */
    val pastOrPresent: PastOrPresent = PastOrPresent(message = MESSAGE),
    /** Regex constraint; the validated object's type must be CharSequence or a subclass. */
    val pattern: Pattern = Pattern(regexp = "", message = MESSAGE),
    /** Positive-number constraint; the validated object's type must be one of the following or a subclass: CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount. */
    val positive: Positive = Positive(message = MESSAGE),
    /** Non-negative constraint; the validated object's type must be one of the following or a subclass: CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount. */
    val positiveOrZero: PositiveOrZero = PositiveOrZero(message = MESSAGE),
    /**
     * Size constraint; the validated object's type must be one of the following or a subclass:
     * CharSequence, Array<*>, Collection<*>, DoubleArray, IntArray, LongArray, CharArray, FloatArray, BooleanArray, ByteArray, ShortArray, Map<*, *>.
     */
    val size: Size = Size(message = MESSAGE),
    /**
     * Maximum-size constraint, equivalent to a Size that only specifies max.
     * The validated object's type must be one of the following or a subclass:
     * CharSequence, Array<*>, Collection<*>, DoubleArray, IntArray, LongArray, CharArray, FloatArray, BooleanArray, ByteArray, ShortArray, Map<*, *>.
     */
    val maxSize: MaxSize = MaxSize(0, message = MESSAGE),

    // Constraints defined by hibernate
    /** String code-point length (actual character count) constraint; the validated object's type must be CharSequence or a subclass. */
    val codePointLength: CodePointLength = CodePointLength(message = MESSAGE),
    /** Credit card number constraint; the validated object's type must be CharSequence or a subclass. */
    val creditCardNumber: CreditCardNumber = CreditCardNumber(message = MESSAGE),
    /** Currency amount constraint; the validated object's type must be MonetaryAmount or a subclass. */
    val currency: Currency = Currency(message = MESSAGE),
    /** EAN product barcode constraint; the validated object's type must be CharSequence or a subclass. */
    val ean: EAN = EAN(message = MESSAGE),
    /** Book barcode constraint; the validated object's type must be CharSequence or a subclass. */
    val isbn: ISBN = ISBN(message = MESSAGE),
    /** String length constraint; the validated object's type must be CharSequence or a subclass. */
    val length: Length = Length(message = MESSAGE),
    /** Maximum length constraint, equivalent to a Length that only specifies max; the validated object's type must be CharSequence or a subclass. */
    val maxLength: MaxLength = MaxLength(0, message = MESSAGE),
    /**
     * Fixed-length constraint, equivalent to a Size with equal min and max.
     * The validated object's type must be one of the following or a subclass:
     * CharSequence, Array<*>, Collection<*>, DoubleArray, IntArray, LongArray, CharArray, FloatArray, BooleanArray, ByteArray, ShortArray, Map<*, *>.
     */
    val fixedLength: FixedLength = FixedLength(0, message = MESSAGE),
    /** String Luhn-algorithm (mod-10) constraint; usable for detecting bank cards and credit cards. The validated object's type must be CharSequence or a subclass. */
    val luhnCheck: LuhnCheck = LuhnCheck(message = MESSAGE),
    /** String mod-10 algorithm constraint; usable for detecting bank cards and credit cards. The validated object's type must be CharSequence or a subclass. */
    val mod10Check: Mod10Check = Mod10Check(message = MESSAGE),
    /** String mod-11 algorithm constraint; the validated object's type must be CharSequence or a subclass. */
    val mod11Check: Mod11Check = Mod11Check(message = MESSAGE),
    /** Parameter script assertion constraint; the validated object's type must be an object array. */
    val parameterScriptAssert: ParameterScriptAssert = ParameterScriptAssert(lang = "", script = "", message = MESSAGE),
    /** Range constraint; the validated object's type must be one of the following or a subclass: CharSequence, Double, Integer, Long, Float, Byte, Short, BigDecimal, BigInteger, Number, MonetaryAmount. */
    val range: Range = Range(message = MESSAGE),
//    /**  */
//    val scriptAssert: ScriptAssert = ScriptAssert(lang = "", script = ""), // Not yet supported by Kudos
    /** Unique-elements collection constraint; the validated object's type must be Collection or a subclass. */
    val uniqueElements: UniqueElements = UniqueElements(message = MESSAGE),
    /** URL constraint; the validated object's type must be CharSequence or a subclass. */
    val url: URL = URL(message = MESSAGE),

    // Constraints defined by kudos
    /** "At least N" constraint; the validated object may be of any type. */
    val atLeast: AtLeast = AtLeast([], message = MESSAGE),
    /** Chinese resident ID card number constraint; the validated object's type must be CharSequence or a subclass. */
    val cnIdCardNo: CnIdCardNo = CnIdCardNo(message = MESSAGE),
    /** Comparison constraint; both compared objects' types must implement the Comparable interface and be of the same type; supports Array<*> type, but the two arrays must be the same size and every array element must implement the Comparable interface. */
    val compare: Compare = Compare("", message = MESSAGE),
    /** Custom-logic constraint; the validated object may be of any type. */
    val custom: Custom = Custom(IBeanValidator::class, message = MESSAGE),
    /** String date-time constraint; the validated object's type must be CharSequence or a subclass. */
    val dateTime: DateTime = DateTime("", message = MESSAGE),
    /** Dictionary enum code constraint; the validated object's type must be CharSequence or a subclass. */
    val dictEnumItemCode: DictEnumItemCode = DictEnumItemCode(IDictEnum::class, message = MESSAGE),
    /** Not-null-dependent constraint; the validated object may be of any type. */
    val notNullOn: NotNullOn = NotNullOn(Depends([]), message = MESSAGE),
    /** Series constraint; the validated object's type must be one of the following or a subclass: List<*>, Array<*>. */
    val series: Series = Series(message = MESSAGE),
    /** Pattern match against a built-in [RegExpEnum]; the validated object's type must be CharSequence or a subclass. */
    val matches: Matches = Matches(value = RegExpEnum.VAR_NAME, message = MESSAGE),

//  val each: Each = Each(Constraints(), message = MESSAGE),  // Would cause circular reference, and it is itself a composite constraint, so there is no need to make it a sub-constraint of Constraints
//  val exist: Exist = Exist(Constraints(), message = MESSAGE),  // Would cause circular reference, and it is itself a composite constraint, so there is no need to make it a sub-constraint of Constraints


    /**
     * Error message; only meaningful when andOr is Or.
     */
    val message: String = "io.kudos.base.bean.validation.constraint.annotations.Constraints.message",

    /**
     * The group classes this validation rule belongs to; groups allow filtering validation rules or ordering validation sequence. The default value must be an empty array.
     * Validation groups let you choose which constraints to apply during validation. In some scenarios (such as a wizard) you can then pick the constraints relevant to each step for that step.
     * Validation groups are passed as varargs to validate, validateProperty and validateValue. If a constraint belongs to multiple groups, the order in which those groups are validated is unpredictable.
     * If a constraint is not assigned to any group, it is grouped into the default group (jakarta.validation.groups.Default).
     * @GroupSequence defines the validation order between groups; usage notes:
     * 1. When applied to a class, it must not contain the jakarta.validation.groups.Default::class group; this is allowed on an interface.
     * 2. When applied to a class, it must include the group of the Class of the Bean to be validated.
     * @GroupSequenceProvider dynamically redefines the default group based on object state; the groups returned by the implementation must contain the group of the Class of the Bean to be validated (because if the `Default` group validates T,
     * the actual instance under validation is passed to this class to determine the default group sequence).
     * Note: when validating with a group sequence, if a group earlier in the sequence fails validation, later groups are no longer validated!
     * Note: constraint validation within the same group is unordered.
     */
    val groups: Array<KClass<*>> = [],

    /** The payload of the constraint annotation (typically used to associate some metadata with the constraint; a common use is to express the severity of the validation result with the payload) */
    val payload: Array<KClass<out Payload>> = []

) {

    companion object {
        /**
         * Placeholder default value for a sub-constraint's `message` field.
         * The validator uses `message == MESSAGE` to determine whether the user has actually enabled the sub-constraint,
         * so every sub-constraint shares this placeholder value when not explicitly declared.
         */
        const val MESSAGE = "TEMP_MSG" // Used to determine which sub-constraints the user has defined
    }

}
