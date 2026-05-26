package io.kudos.ability.data.rdb.ktorm.support

import java.math.BigDecimal
import java.sql.Blob
import java.sql.Clob
import java.sql.Date
import java.sql.Time
import java.sql.Timestamp
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.MonthDay
import java.time.Year
import java.time.YearMonth
import java.util.UUID
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Unit tests for the [KtormSqlType] mapping function.
 *
 * This mapping underpins how the code generator / Ktorm adapter assembles column-binding function names —
 * regression test once so changes (such as renaming `BigDecimal` to `numeric`) don't slip through.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
internal class KtormSqlTypeTest {

    @Test
    fun primitives() {
        assertEquals("boolean", KtormSqlType.getFunName(Boolean::class))
        assertEquals("int", KtormSqlType.getFunName(Int::class))
        assertEquals("long", KtormSqlType.getFunName(Long::class))
        assertEquals("float", KtormSqlType.getFunName(Float::class))
        assertEquals("double", KtormSqlType.getFunName(Double::class))
    }

    @Test
    fun stringsAndBinary() {
        assertEquals("varchar", KtormSqlType.getFunName(String::class))
        assertEquals("text", KtormSqlType.getFunName(Clob::class))
        assertEquals("blob", KtormSqlType.getFunName(Blob::class))
        assertEquals("bytes", KtormSqlType.getFunName(ByteArray::class))
    }

    @Test
    fun numericAndUuid() {
        assertEquals("decimal", KtormSqlType.getFunName(BigDecimal::class))
        assertEquals("uuid", KtormSqlType.getFunName(UUID::class))
    }

    @Test
    fun jdbcDateTime() {
        assertEquals("jdbcTimestamp", KtormSqlType.getFunName(Timestamp::class))
        assertEquals("jdbcDate", KtormSqlType.getFunName(Date::class))
        assertEquals("jdbcTime", KtormSqlType.getFunName(Time::class))
    }

    @Test
    fun javaTimeDateTime() {
        assertEquals("timestamp", KtormSqlType.getFunName(Instant::class))
        assertEquals("datetime", KtormSqlType.getFunName(LocalDateTime::class))
        assertEquals("date", KtormSqlType.getFunName(LocalDate::class))
        assertEquals("time", KtormSqlType.getFunName(LocalTime::class))
        assertEquals("monthDay", KtormSqlType.getFunName(MonthDay::class))
        assertEquals("yearMonth", KtormSqlType.getFunName(YearMonth::class))
        assertEquals("year", KtormSqlType.getFunName(Year::class))
    }

    @Test
    fun enumDirectClass() {
        assertEquals("enum", KtormSqlType.getFunName(Enum::class))
    }

    @Test
    fun enumSubclass() {
        assertEquals("enum", KtormSqlType.getFunName(TestEnum::class))
    }

    @Test
    fun unknownTypeReturnsEmpty() {
        // Uncovered types fall through to the else branch; the current implementation returns an empty string (not an error). Code-generation scenarios usually don't hit this type.
        assertEquals("", KtormSqlType.getFunName(Any::class))
    }

    /**
     * Test target for enum-subclass mapping.
     *
     * @author K
     * @author AI: Codex
     * @since 1.0.0
     */
    private enum class TestEnum {
        ONE
    }
}
