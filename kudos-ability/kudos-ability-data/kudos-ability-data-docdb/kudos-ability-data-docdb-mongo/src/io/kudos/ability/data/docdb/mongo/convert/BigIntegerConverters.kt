package io.kudos.ability.data.docdb.mongo.convert

import org.springframework.core.convert.converter.Converter
import org.springframework.data.convert.ReadingConverter
import org.springframework.data.convert.WritingConverter
import java.math.BigInteger

/**
 * Spring Data converters that round-trip [BigInteger] values through MongoDB as Strings.
 *
 * BSON has no native arbitrary-precision integer codec: a [Long] caps at 64 bits, [Double] loses
 * precision past 2^53, and `Decimal128` tops out at 34 decimal digits. Storing as String keeps
 * full precision (useful for token amounts, large user ids, public-key derivatives, etc.) at the
 * cost of giving up `$gt` / `$lt` numeric comparisons on the field — those would compare
 * lexically. Apps that need numeric range queries on `BigInteger` columns shouldn't enable this
 * converter for those fields.
 *
 * Registered as a bean by [io.kudos.ability.data.docdb.mongo.init.MongoAutoConfiguration] when
 * `kudos.ability.docdb.mongo.big-integer-as-string` is true (default true).
 *
 * Ported from soul's `BigIntegerConverters` with one behavioural fix: soul's reading converter
 * returns null on blank input, which silently masks malformed data. The kudos port lets the
 * `NumberFormatException` propagate so a stray empty/blank stored value surfaces loudly instead
 * of becoming a null in business code.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
object BigIntegerConverters {

    @WritingConverter
    object BigIntegerToString : Converter<BigInteger, String> {
        override fun convert(source: BigInteger): String = source.toString()
    }

    @ReadingConverter
    object StringToBigInteger : Converter<String, BigInteger> {
        override fun convert(source: String): BigInteger = BigInteger(source)
    }
}
