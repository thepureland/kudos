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
 * [KtormSqlType] 映射函数单元测试。
 *
 * 该映射是代码生成器 / Ktorm 适配器拼接列绑定函数名的基础——回归一次防止
 * 误改映射条目（如把 `BigDecimal` 写错成 `numeric` 之类）。
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
        // 直接拿 Enum::class 做参数才会命中 "enum" 分支；具体枚举子类（如 MyEnum::class）目前不会命中——
        // 见 README 已知限制部分。
        assertEquals("enum", KtormSqlType.getFunName(Enum::class))
    }

    @Test
    fun unknownTypeReturnsEmpty() {
        // 未覆盖的类型走 else 分支，目前实现返回空字符串（非抛错）。代码生成场景一般用不到这种类型。
        assertEquals("", KtormSqlType.getFunName(Any::class))
    }
}
