package io.kudos.ability.data.rdb.ktorm.support

import java.util.UUID
import kotlin.reflect.KClass

/**
 * Mapping from Kotlin types to Ktorm SQL-type function names (used for column binding in DAOs).
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object KtormSqlType {

    /**
     * Returns the Ktorm SQL-type function name corresponding to the Kotlin type.
     *
     * @param clazz Kotlin type
     * @return Ktorm SQL-type function name
     * @author K
     * @since 1.0.0
     */
    fun getFunName(clazz: KClass<*>): String =
        when (clazz) {
            Boolean::class -> "boolean"
            Int::class -> "int"
            Long::class -> "long"
            Float::class -> "float"
            Double::class -> "double"
            java.math.BigDecimal::class -> "decimal"
            String::class -> "varchar"
            java.sql.Clob::class -> "text"
            java.sql.Blob::class -> "blob"
            ByteArray::class -> "bytes"
            java.sql.Timestamp::class -> "jdbcTimestamp"
            java.sql.Date::class -> "jdbcDate"
            java.sql.Time::class -> "jdbcTime"
            java.time.Instant::class -> "timestamp"
            java.time.LocalDateTime::class -> "datetime"
            java.time.LocalDate::class -> "date"
            java.time.LocalTime::class -> "time"
            java.time.MonthDay::class -> "monthDay"
            java.time.YearMonth::class -> "yearMonth"
            java.time.Year::class -> "year"
            UUID::class -> "uuid"
            else -> if (Enum::class.java.isAssignableFrom(clazz.java)) "enum" else ""
        }

}
