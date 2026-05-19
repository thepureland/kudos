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
 * 验证器工厂
 *
 * 根据注解类型查找对应的 ConstraintValidator。内部基于一个 [BUILDERS] 注册表：
 * - 简单注解（无值类型分发）直接列在表里，一行一个
 * - 数值/日期/Size 这三类共享同一组按值类型分发的工厂函数（[numericBound] / [dateBound] / [sizeBound]），
 *   避免在每个注解里写一遍相同的 when 分支
 * - 复合注解（如 Range = Min + Max、CreditCardNumber → LuhnCheck）使用就地 lambda
 *
 * 新增一种注解只需在 [BUILDERS] 加一项，无须修改一个 350 行的 when。
 *
 * ## 实例缓存
 *
 * 返回的 validator 列表按 (annotation, value 的运行时 class) 缓存，依赖 JDK Annotation 的内容相等契约：
 * 相同 attribute 的两个 `@Min(10)` 命中同一个 cache key。
 *
 * 这能消除重复的 `new XxxValidator()` 与 ValidatorFactory 层的 `initialize` 调用，对走反射构造内层
 * 注解的 Range / CreditCardNumber 收益尤为明显（短路 [createAnnotationByNamedArgs]）。
 * 注意调用方 ConstraintsValidator 仍会再次 initialize（用 HV 的 descriptor + initCtx 重载），
 * 缓存不影响其语义。
 *
 * @author K
 * @since 1.0.0
 */
object ValidatorFactory {

    /** validator 实例缓存：按 (annotation, value 的运行时 class) 命中，避免重复构造与 initialize */
    private val CACHE: MutableMap<CacheKey, List<ConstraintValidator<*, *>>> = ConcurrentHashMap()

    /**
     * 返回校验规则注解对应的验证器实例。
     *
     * @param annotation 校验规则注解
     * @param value 待校验的值
     * @return 验证器列表；不支持的注解返回空列表（Range 等复合注解会返回多个）
     */
    fun getValidator(annotation: Annotation, value: Any): List<ConstraintValidator<*, *>> {
        val key = CacheKey(annotation, value::class.java)
        return CACHE.computeIfAbsent(key) { build(annotation, value) }
    }

    /**
     * 仅用于测试：清空缓存。生产代码请不要依赖这个方法。
     */
    internal fun clearCacheForTest() {
        CACHE.clear()
    }

    /**
     * 缓存未命中时的实际构造路径：在 [BUILDERS] 注册表里查 annotation 对应的工厂闭包并执行。
     *
     * @param annotation 当前注解
     * @param value 被校验的值（用于按运行时类型分发）
     * @return 该注解对应的 validator 列表，未注册的注解返回空列表
     * @author K
     * @since 1.0.0
     */
    private fun build(annotation: Annotation, value: Any): List<ConstraintValidator<*, *>> {
        val builder = BUILDERS[annotation.annotationClass] ?: return emptyList()
        return builder(annotation, value)
    }

    /**
     * validator 缓存的复合键。
     * annotation 本身参与 hashCode/equals 依赖 JDK Annotation 的“同 attribute 即相等”契约。
     *
     * @property annotation 当前注解实例
     * @property valueClass 被校验值的运行时类，用于不同类型分发的 validator 各自缓存
     */
    private data class CacheKey(val annotation: Annotation, val valueClass: Class<*>)

    // ----------------------------- helpers -----------------------------

    /**
     * 抹掉 ConstraintValidator 的泛型实参，统一调用 [ConstraintValidator.initialize] 并返回自身，
     * 便于在工厂闭包里以链式风格构造列表。
     *
     * @param validator 已实例化的校验器
     * @param annotation 用来初始化的注解
     * @return 同一个 validator 实例
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

    /** 不依赖 value 类型、不需要 initialize 的单一校验器 */
    private fun raw(create: () -> ConstraintValidator<*, *>): ValidatorBuilder =
        { _, _ -> listOf(create()) }

    /** 不依赖 value 类型、需要 initialize 的单一校验器 */
    private fun simple(create: () -> ConstraintValidator<*, *>): ValidatorBuilder =
        { annotation, _ -> listOf(initialize(create(), annotation)) }

    /**
     * 数值型约束的分发模板（DecimalMax / DecimalMin / Max / Min / Negative / NegativeOrZero / Positive / PositiveOrZero）。
     * 按 value 的实际类型挑选对应的内置校验器，统一调用 initialize(annotation)。
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
            else -> error("${name}约束注解不支持【${value::class}】类型的校验！")
        }
        listOf(initialize(factory(), annotation))
    }

    /** 日期/时间型约束的分发模板（Future / FutureOrPresent / Past / PastOrPresent）。 */
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
            else -> error("${name}约束注解不支持【${value::class}】类型的校验！")
        }
        listOf(initialize(factory(), annotation))
    }

    /** 集合/数组类约束的分发模板（NotEmpty / Size）。 */
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
            else -> error("${name}约束注解不支持【${value::class}】类型的校验！")
        }
        listOf(initialize(factory(), annotation))
    }

    /**
     * 反射构造注解实例：按形参名匹配 [namedArgs]，没匹配上的参数使用默认值。
     * 用于把复合约束（Range、CreditCardNumber）拆解成内部更小的约束（Min/Max、LuhnCheck）后转交对应 validator。
     *
     * @param A 目标注解类型
     * @param annotationClass 目标注解的 KClass
     * @param namedArgs 形参名到实参值的映射
     * @return 实例化好的注解
     * @throws IllegalStateException 找不到注解构造器时
     * @author K
     * @since 1.0.0
     */
    private fun <A : Annotation> createAnnotationByNamedArgs(
        annotationClass: KClass<A>,
        namedArgs: Map<String, Any>
    ): A {
        val constructor = annotationClass.constructors.firstOrNull()
            ?: error("无法找到注解【${annotationClass.qualifiedName}】的构造函数")
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
     * 注解类型到 validator 构造闭包的注册表。
     * 新增一种注解仅需在此表追加一项；按值类型分发的注解走 [numericBound] / [dateBound] / [sizeBound] 模板，
     * 复合注解（Range = Min+Max、CreditCardNumber → LuhnCheck）使用就地 lambda 拆解。
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
                else -> error("Digits约束注解不支持【${value::class}】类型的校验！")
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
                error("Matches约束注解不支持【${value::class}】类型的校验！")
            }
            listOf(initialize(MatchesValidator(), annotation))
        }
    }

}
