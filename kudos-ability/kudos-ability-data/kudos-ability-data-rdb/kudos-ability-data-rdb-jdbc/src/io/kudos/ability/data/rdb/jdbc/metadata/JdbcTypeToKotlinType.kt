package io.kudos.ability.data.rdb.jdbc.metadata

import org.soul.ability.data.rdb.jdbc.metadata.RdbTypeEnum
import java.sql.Types
import java.util.*
import kotlin.reflect.KClass

/**
 * jdbc类型和kotlin类型的映射
 *
 * @author K
 * @since 1.0.0
 */
object JdbcTypeToKotlinType {

    private val defaultMapping: Map<Int, KClass<*>> = mapOf(
        Types.ARRAY to Array<Any>::class,
        Types.BIGINT to Long::class,
        Types.BINARY to Array<Byte>::class,
        Types.BIT to Boolean::class,
        Types.BLOB to java.sql.Blob::class,
        Types.BOOLEAN to Boolean::class,
        Types.CHAR to String::class,
        Types.CLOB to java.sql.Clob::class,
        Types.DATALINK to Any::class,
        Types.DATE to java.time.LocalDate::class,
        Types.DECIMAL to java.math.BigDecimal::class,
        Types.DISTINCT to Any::class,
        Types.DOUBLE to Double::class,
        Types.FLOAT to Double::class,
        Types.INTEGER to Int::class,
        Types.JAVA_OBJECT to Any::class,
        Types.LONGNVARCHAR to String::class,
        Types.LONGVARBINARY to Array<Byte>::class,
        Types.LONGVARCHAR to String::class,
        Types.NCHAR to String::class,
        Types.NCLOB to java.sql.Clob::class,
        Types.NULL to Nothing::class,
        Types.NUMERIC to Double::class,
        Types.NVARCHAR to String::class,
        Types.OTHER to Any::class,
        Types.REAL to Float::class,
        Types.REF to java.sql.Ref::class,
        Types.REF_CURSOR to Any::class,
        Types.ROWID to java.sql.RowId::class,
//        Types.SMALLINT to Short::class,
        Types.SMALLINT to Int::class,
        Types.SQLXML to java.sql.SQLXML::class,
        Types.STRUCT to Any::class,
        Types.TIME to java.time.LocalTime::class,
        Types.TIMESTAMP to java.time.LocalDateTime::class,
        Types.TIMESTAMP_WITH_TIMEZONE to java.time.LocalDateTime::class,
        Types.TIME_WITH_TIMEZONE to java.time.LocalDateTime::class,
//        Types.TINYINT to Byte::class,
        Types.TINYINT to Int::class,
        Types.VARBINARY to Array<Byte>::class,
        Types.VARCHAR to String::class
    )

    /**
     * 返回列的指定关系型数据库对应的Kotlin类型
     *
     * @param rdbType 关系型数据库枚举
     * @param column 列
     * @return Kotlin类型
     * @author K
     * @since 1.0.0
     */
    fun getKotlinType(rdbType: RdbTypeEnum, column: Column): KClass<*> {
        val jdbcType = column.jdbcTypeName.uppercase(Locale.getDefault())
        return when (rdbType) {
            RdbTypeEnum.H2 -> {
                when (jdbcType) {
                    "BOOLEAN", "BIT", "BOOL" -> Boolean::class
                    "TINYINT" -> Int::class // Byte::class
                    "SMALLINT", "INT2", "YEAR" -> Int::class // Short::class
                    "INT", "INTEGER", "MEDIUMINT", "INT4", "SIGNED" -> Int::class
                    "BIGINT", "INT8", "IDENTITY" -> Long::class
                    "REAL" -> Float::class
                    "DOUBLE", "PRECISION", "FLOAT", "FLOAT4", "FLOAT8" -> Double::class
                    "DECIMAL", "NUMBER", "DEC", "NUMERI" -> java.math.BigDecimal::class
                    "VARCHAR", "LONGVARCHAR", "VARCHAR2", "NVARCHAR", "NVARCHAR2", "VARCHAR_CASESENSITIVE", "VARCHAR_IGNORECASE", "CHAR", "CHARACTER", "NCHAR", "CHARACTER VARYING" -> String::class
                    "BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB", "IMAGE", "OID" -> java.sql.Blob::class
                    "CLOB", "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT", "NTEXT", "NCLOB" -> java.sql.Clob::class
                    "DATE" -> java.time.LocalDate::class
                    "TIME" -> java.time.LocalTime::class
                    "TIMESTAMP", "DATETIME", "SMALLDATETIME" -> java.time.LocalDateTime::class
                    "BINARY", "VARBINARY", "LONGVARBINARY", "RAW", "BYTEA" -> Array<Byte>::class
                    "UUID" -> java.util.UUID::class
                    "ARRAY" -> Array<Any>::class
                    "JSON", "JSONB" -> String::class
                    "GEOMETRY" -> ByteArray::class
                    "OTHER" -> Any::class
                    else -> Any::class
                }
            }
            RdbTypeEnum.MYSQL -> {
                when (jdbcType) {
                    "BIT" -> when (column.length) {
                        1    -> Boolean::class
                        else -> ByteArray::class
                    }
                    "TINYINT", "SMALLINT", "MEDIUMINT", "BOOLEAN" -> Int::class
                    "INTEGER", "ID" -> Long::class
                    "FLOAT" -> Float::class
                    "DOUBLE" -> Double::class
                    "BIGINT" -> java.math.BigInteger::class
                    "DECIMAL" -> java.math.BigDecimal::class
                    "YEAR" -> Int::class
                    "VARCHAR", "CHAR", "TEXT", "ENUM", "SET", "JSON" -> String::class
                    "BLOB", "GEOMETRY", "POINT", "LINESTRING", "POLYGON" -> ByteArray::class
                    "DATE" -> java.time.LocalDate::class
                    "TIME" -> java.time.LocalTime::class
                    "DATETIME", "TIMESTAMP" -> java.time.LocalDateTime::class
                    else -> Any::class
                }
            }
            RdbTypeEnum.ORACLE -> {
                when {
                    jdbcType in listOf("BOOL", "BOOLEAN", "NUMBER(1)", "NUMBER(1,0)") -> Boolean::class
                    jdbcType in listOf("NUMBER(2)", "NUMBER(2,0)") -> Int::class
                    jdbcType.matches(Regex("NUMBER\\([3-4],0?\\)")) -> Int::class
                    jdbcType.matches(Regex("NUMBER\\((?:5|6|7|8|9|10),0?\\)")) -> Int::class
                    jdbcType.matches(Regex("NUMBER\\((?:11|12|13|14|15|16|17|18|19),0?\\)")) -> Long::class
                    jdbcType in listOf("FLOAT", "BINARY_FLOAT") -> Float::class
                    jdbcType in listOf("DOUBLE", "BINARY_DOUBLE") -> Double::class
                    jdbcType.startsWith("NUMBER") -> java.math.BigDecimal::class
                    jdbcType in listOf("DEC", "DECIMAL", "DOUBLE PRECISION") -> java.math.BigDecimal::class
                    jdbcType in listOf("VARCHAR2", "CHAR", "LONG", "NVARCHAR2", "CHARACTER", "VARCHAR") -> String::class
                    jdbcType in listOf("BFILE", "RAW", "LONGRAW", "LONG VARCHAR") -> ByteArray::class
                    jdbcType == "BLOB" -> java.sql.Blob::class
                    jdbcType in listOf("CLOB", "NCLOB") -> java.sql.Clob::class
                    jdbcType == "DATE" -> java.time.LocalDate::class
                    jdbcType == "TIME" -> java.time.LocalTime::class
                    jdbcType in listOf("DATETIME", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE") -> java.time.LocalDateTime::class
                    jdbcType.startsWith("INTERVAL") -> java.time.Duration::class
                    jdbcType in listOf("REF CURSOR") -> java.sql.ResultSet::class
                    jdbcType in listOf("ROWID", "UROWID") -> String::class
                    jdbcType == "XMLTYPE" -> String::class
                    jdbcType in listOf("OBJECT", "REF") -> java.sql.Struct::class
                    else -> Any::class
                }
            }
            RdbTypeEnum.POSTGRESQL -> {
                when (jdbcType) {
                    "BIT", "BOOL" -> Boolean::class
                    "INT2", "INT4", "SMALLSERIAL", "SERIAL" -> Int::class
                    "INT8", "BIGSERIAL" -> Long::class
                    "FLOAT4" -> Float::class
                    "FLOAT8", "MONEY" -> Double::class
                    "NUMERIC" -> java.math.BigDecimal::class
                    "UUID" -> java.util.UUID::class
                    "VARCHAR", "BPCHAR", "TEXT" -> String::class
                    "JSON", "JSONB", "XML" -> String::class
                    "DATE" -> java.time.LocalDate::class
                    "TIME" -> java.time.LocalTime::class
                    "TIME WITH TIME ZONE" -> java.time.OffsetTime::class
                    "TIMESTAMP", "TIMESTAMP WITHOUT TIMEZONE" -> java.time.LocalDateTime::class
                    "TIMESTAMP WITH TIMEZONE" -> java.time.OffsetDateTime::class
                    "INTERVAL" -> java.time.Duration::class
                    "BYTEA" -> Array<Byte>::class
                    "CIDR", "INET", "MACADDR", "BOX", "CIRCLE", "LINE", "LSEG", "PATH", "POINT", "POLYGON", "VARBIT" -> Any::class
                    else -> Any::class
                }
            }
            RdbTypeEnum.SQLITE -> {
                when (jdbcType) {
                    "BOOLEAN" -> Boolean::class
                    "INT", "INT2", "INTEGER", "TINYINT", "SMALLINT", "MEDIUMINT", "SMALLSERIAL", "SERIAL" -> Int::class
                    "BIGINT", "UNSIGNED BIG INT", "INT8", "BIGSERIAL" -> Long::class
                    "FLOAT" -> Float::class
                    "REAL", "DOUBLE", "DOUBLE PRECISION", "NUMERIC" -> Double::class
                    "DECIMAL" -> java.math.BigDecimal::class
                    "CHARACTER", "VARCHAR", "VARYING CHARACTER", "NCHAR", "NATIVE CHARACTER", "NVARCHAR", "TEXT" -> String::class
                    "BLOB" -> java.sql.Blob::class
                    "CLOB" -> java.sql.Clob::class
                    "DATE" -> java.time.LocalDate::class
                    "TIME" -> java.time.LocalTime::class
                    "DATETIME" -> java.time.LocalDateTime::class
                    else -> Any::class
                }
            }
            else -> {
                defaultMapping[column.jdbcType] ?: error("未支持JdbcType: \${column.jdbcType}")
            }
        }
    }


}