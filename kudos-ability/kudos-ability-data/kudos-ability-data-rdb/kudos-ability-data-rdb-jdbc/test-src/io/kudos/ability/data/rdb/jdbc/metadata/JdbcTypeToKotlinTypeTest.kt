package io.kudos.ability.data.rdb.jdbc.metadata

import java.math.BigDecimal
import java.math.BigInteger
import java.sql.Blob
import java.sql.Clob
import java.sql.Ref
import java.sql.ResultSet
import java.sql.RowId
import java.sql.SQLXML
import java.sql.Struct
import java.sql.Types
import java.time.Duration
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.util.UUID
import kotlin.reflect.KClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Branch-complete unit tests for [JdbcTypeToKotlinType.getKotlinType].
 *
 * One assertion per `when` arm for every database branch (H2 / MySQL / Oracle / PostgreSQL /
 * SQLite), the defaultMapping fallback used by the remaining vendors, lower-case type-name
 * normalization, the MySQL BIT length distinction and the unsupported-jdbcType failure mode.
 *
 * @author K
 * @since 1.0.0
 */
internal class JdbcTypeToKotlinTypeTest {

    private fun col(typeName: String, jdbcType: Int = Types.OTHER, length: Int? = null) =
        Column().apply {
            name = "c"
            jdbcTypeName = typeName
            this.jdbcType = jdbcType
            this.length = length
        }

    private fun assertType(rdbType: RdbTypeEnum, typeName: String, expected: KClass<*>, length: Int? = null) {
        assertEquals(
            expected, JdbcTypeToKotlinType.getKotlinType(rdbType, col(typeName, length = length)),
            "$rdbType / $typeName"
        )
    }

    // ----- H2 -----

    @Test
    fun h2_mappings() {
        listOf("BOOLEAN", "BIT", "BOOL").forEach { assertType(RdbTypeEnum.H2, it, Boolean::class) }
        assertType(RdbTypeEnum.H2, "TINYINT", Int::class)
        listOf("SMALLINT", "INT2", "YEAR").forEach { assertType(RdbTypeEnum.H2, it, Int::class) }
        listOf("INT", "INTEGER", "MEDIUMINT", "INT4", "SIGNED").forEach { assertType(RdbTypeEnum.H2, it, Int::class) }
        listOf("BIGINT", "INT8", "IDENTITY").forEach { assertType(RdbTypeEnum.H2, it, Long::class) }
        assertType(RdbTypeEnum.H2, "REAL", Float::class)
        listOf("DOUBLE", "PRECISION", "FLOAT", "FLOAT4", "FLOAT8").forEach {
            assertType(RdbTypeEnum.H2, it, Double::class)
        }
        listOf("DECIMAL", "NUMBER", "DEC", "NUMERI").forEach { assertType(RdbTypeEnum.H2, it, BigDecimal::class) }
        listOf(
            "VARCHAR", "LONGVARCHAR", "VARCHAR2", "NVARCHAR", "NVARCHAR2", "VARCHAR_CASESENSITIVE",
            "VARCHAR_IGNORECASE", "CHAR", "CHARACTER", "NCHAR", "CHARACTER VARYING"
        ).forEach { assertType(RdbTypeEnum.H2, it, String::class) }
        listOf("BLOB", "TINYBLOB", "MEDIUMBLOB", "LONGBLOB", "IMAGE", "OID").forEach {
            assertType(RdbTypeEnum.H2, it, Blob::class)
        }
        listOf("CLOB", "TINYTEXT", "TEXT", "MEDIUMTEXT", "LONGTEXT", "NTEXT", "NCLOB").forEach {
            assertType(RdbTypeEnum.H2, it, Clob::class)
        }
        assertType(RdbTypeEnum.H2, "DATE", LocalDate::class)
        assertType(RdbTypeEnum.H2, "TIME", LocalTime::class)
        listOf("TIMESTAMP", "DATETIME", "SMALLDATETIME").forEach {
            assertType(RdbTypeEnum.H2, it, LocalDateTime::class)
        }
        listOf("BINARY", "VARBINARY", "LONGVARBINARY", "RAW", "BYTEA").forEach {
            assertType(RdbTypeEnum.H2, it, Array<Byte>::class)
        }
        assertType(RdbTypeEnum.H2, "UUID", UUID::class)
        assertType(RdbTypeEnum.H2, "ARRAY", Array<Any>::class)
        listOf("JSON", "JSONB").forEach { assertType(RdbTypeEnum.H2, it, String::class) }
        assertType(RdbTypeEnum.H2, "GEOMETRY", ByteArray::class)
        assertType(RdbTypeEnum.H2, "OTHER", Any::class)
        assertType(RdbTypeEnum.H2, "WHO_KNOWS", Any::class)
    }

    @Test
    fun typeNameIsCaseInsensitive() {
        assertType(RdbTypeEnum.H2, "varchar", String::class)
        assertType(RdbTypeEnum.H2, "Boolean", Boolean::class)
    }

    // ----- MySQL -----

    @Test
    fun mysql_mappings() {
        assertType(RdbTypeEnum.MYSQL, "BIT", Boolean::class, length = 1)
        assertType(RdbTypeEnum.MYSQL, "BIT", ByteArray::class, length = 8)
        assertType(RdbTypeEnum.MYSQL, "BIT", ByteArray::class, length = null)
        listOf("TINYINT", "SMALLINT", "MEDIUMINT", "BOOLEAN").forEach {
            assertType(RdbTypeEnum.MYSQL, it, Int::class)
        }
        listOf("INTEGER", "ID").forEach { assertType(RdbTypeEnum.MYSQL, it, Long::class) }
        assertType(RdbTypeEnum.MYSQL, "FLOAT", Float::class)
        assertType(RdbTypeEnum.MYSQL, "DOUBLE", Double::class)
        assertType(RdbTypeEnum.MYSQL, "BIGINT", BigInteger::class)
        assertType(RdbTypeEnum.MYSQL, "DECIMAL", BigDecimal::class)
        assertType(RdbTypeEnum.MYSQL, "YEAR", Int::class)
        listOf("VARCHAR", "CHAR", "TEXT", "ENUM", "SET", "JSON").forEach {
            assertType(RdbTypeEnum.MYSQL, it, String::class)
        }
        listOf("BLOB", "GEOMETRY", "POINT", "LINESTRING", "POLYGON").forEach {
            assertType(RdbTypeEnum.MYSQL, it, ByteArray::class)
        }
        assertType(RdbTypeEnum.MYSQL, "DATE", LocalDate::class)
        assertType(RdbTypeEnum.MYSQL, "TIME", LocalTime::class)
        listOf("DATETIME", "TIMESTAMP").forEach { assertType(RdbTypeEnum.MYSQL, it, LocalDateTime::class) }
        assertType(RdbTypeEnum.MYSQL, "UNKNOWN_TYPE", Any::class)
    }

    // ----- Oracle -----

    @Test
    fun oracle_mappings() {
        listOf("BOOL", "BOOLEAN", "NUMBER(1)", "NUMBER(1,0)").forEach {
            assertType(RdbTypeEnum.ORACLE, it, Boolean::class)
        }
        listOf("NUMBER(2)", "NUMBER(2,0)").forEach { assertType(RdbTypeEnum.ORACLE, it, Int::class) }
        listOf("NUMBER(3,0)", "NUMBER(4,)").forEach { assertType(RdbTypeEnum.ORACLE, it, Int::class) }
        listOf("NUMBER(5,0)", "NUMBER(10,0)", "NUMBER(7,)").forEach {
            assertType(RdbTypeEnum.ORACLE, it, Int::class)
        }
        listOf("NUMBER(11,0)", "NUMBER(19,0)", "NUMBER(15,)").forEach {
            assertType(RdbTypeEnum.ORACLE, it, Long::class)
        }
        listOf("FLOAT", "BINARY_FLOAT").forEach { assertType(RdbTypeEnum.ORACLE, it, Float::class) }
        listOf("DOUBLE", "BINARY_DOUBLE").forEach { assertType(RdbTypeEnum.ORACLE, it, Double::class) }
        // NUMBER without parens (or with precision/scale combos not matched above) -> BigDecimal
        listOf("NUMBER", "NUMBER(20,0)", "NUMBER(10,2)").forEach {
            assertType(RdbTypeEnum.ORACLE, it, BigDecimal::class)
        }
        listOf("DEC", "DECIMAL", "DOUBLE PRECISION").forEach { assertType(RdbTypeEnum.ORACLE, it, BigDecimal::class) }
        listOf("VARCHAR2", "CHAR", "LONG", "NVARCHAR2", "CHARACTER", "VARCHAR").forEach {
            assertType(RdbTypeEnum.ORACLE, it, String::class)
        }
        listOf("BFILE", "RAW", "LONGRAW", "LONG VARCHAR").forEach {
            assertType(RdbTypeEnum.ORACLE, it, ByteArray::class)
        }
        assertType(RdbTypeEnum.ORACLE, "BLOB", Blob::class)
        listOf("CLOB", "NCLOB").forEach { assertType(RdbTypeEnum.ORACLE, it, Clob::class) }
        assertType(RdbTypeEnum.ORACLE, "DATE", LocalDate::class)
        assertType(RdbTypeEnum.ORACLE, "TIME", LocalTime::class)
        listOf("DATETIME", "TIMESTAMP", "TIMESTAMP WITH TIME ZONE", "TIMESTAMP WITH LOCAL TIME ZONE").forEach {
            assertType(RdbTypeEnum.ORACLE, it, LocalDateTime::class)
        }
        assertType(RdbTypeEnum.ORACLE, "INTERVAL DAY TO SECOND", Duration::class)
        assertType(RdbTypeEnum.ORACLE, "REF CURSOR", ResultSet::class)
        listOf("ROWID", "UROWID").forEach { assertType(RdbTypeEnum.ORACLE, it, String::class) }
        assertType(RdbTypeEnum.ORACLE, "XMLTYPE", String::class)
        listOf("OBJECT", "REF").forEach { assertType(RdbTypeEnum.ORACLE, it, Struct::class) }
        assertType(RdbTypeEnum.ORACLE, "SOMETHING_ELSE", Any::class)
    }

    // ----- PostgreSQL -----

    @Test
    fun postgresql_mappings() {
        listOf("BIT", "BOOL").forEach { assertType(RdbTypeEnum.POSTGRESQL, it, Boolean::class) }
        listOf("INT2", "INT4", "SMALLSERIAL", "SERIAL").forEach {
            assertType(RdbTypeEnum.POSTGRESQL, it, Int::class)
        }
        listOf("INT8", "BIGSERIAL").forEach { assertType(RdbTypeEnum.POSTGRESQL, it, Long::class) }
        assertType(RdbTypeEnum.POSTGRESQL, "FLOAT4", Float::class)
        listOf("FLOAT8", "MONEY").forEach { assertType(RdbTypeEnum.POSTGRESQL, it, Double::class) }
        assertType(RdbTypeEnum.POSTGRESQL, "NUMERIC", BigDecimal::class)
        assertType(RdbTypeEnum.POSTGRESQL, "UUID", UUID::class)
        listOf("VARCHAR", "BPCHAR", "TEXT").forEach { assertType(RdbTypeEnum.POSTGRESQL, it, String::class) }
        listOf("JSON", "JSONB", "XML").forEach { assertType(RdbTypeEnum.POSTGRESQL, it, String::class) }
        assertType(RdbTypeEnum.POSTGRESQL, "DATE", LocalDate::class)
        assertType(RdbTypeEnum.POSTGRESQL, "TIME", LocalTime::class)
        assertType(RdbTypeEnum.POSTGRESQL, "TIME WITH TIME ZONE", OffsetTime::class)
        listOf("TIMESTAMP", "TIMESTAMP WITHOUT TIMEZONE").forEach {
            assertType(RdbTypeEnum.POSTGRESQL, it, LocalDateTime::class)
        }
        assertType(RdbTypeEnum.POSTGRESQL, "TIMESTAMP WITH TIMEZONE", OffsetDateTime::class)
        assertType(RdbTypeEnum.POSTGRESQL, "INTERVAL", Duration::class)
        assertType(RdbTypeEnum.POSTGRESQL, "BYTEA", Array<Byte>::class)
        listOf("CIDR", "INET", "MACADDR", "BOX", "CIRCLE", "LINE", "LSEG", "PATH", "POINT", "POLYGON", "VARBIT")
            .forEach { assertType(RdbTypeEnum.POSTGRESQL, it, Any::class) }
        assertType(RdbTypeEnum.POSTGRESQL, "GEOGRAPHY", Any::class)
    }

    // ----- SQLite -----

    @Test
    fun sqlite_mappings() {
        assertType(RdbTypeEnum.SQLITE, "BOOLEAN", Boolean::class)
        listOf("INT", "INT2", "INTEGER", "TINYINT", "SMALLINT", "MEDIUMINT", "SMALLSERIAL", "SERIAL").forEach {
            assertType(RdbTypeEnum.SQLITE, it, Int::class)
        }
        listOf("BIGINT", "UNSIGNED BIG INT", "INT8", "BIGSERIAL").forEach {
            assertType(RdbTypeEnum.SQLITE, it, Long::class)
        }
        assertType(RdbTypeEnum.SQLITE, "FLOAT", Float::class)
        listOf("REAL", "DOUBLE", "DOUBLE PRECISION", "NUMERIC").forEach {
            assertType(RdbTypeEnum.SQLITE, it, Double::class)
        }
        assertType(RdbTypeEnum.SQLITE, "DECIMAL", BigDecimal::class)
        listOf("CHARACTER", "VARCHAR", "VARYING CHARACTER", "NCHAR", "NATIVE CHARACTER", "NVARCHAR", "TEXT")
            .forEach { assertType(RdbTypeEnum.SQLITE, it, String::class) }
        assertType(RdbTypeEnum.SQLITE, "BLOB", Blob::class)
        assertType(RdbTypeEnum.SQLITE, "CLOB", Clob::class)
        assertType(RdbTypeEnum.SQLITE, "DATE", LocalDate::class)
        assertType(RdbTypeEnum.SQLITE, "TIME", LocalTime::class)
        assertType(RdbTypeEnum.SQLITE, "DATETIME", LocalDateTime::class)
        assertType(RdbTypeEnum.SQLITE, "MYSTERY", Any::class)
    }

    // ----- default mapping fallback (other vendors) -----

    @Test
    fun otherVendors_useDefaultJdbcTypeMapping() {
        val expectations = mapOf(
            Types.ARRAY to Array<Any>::class,
            Types.BIGINT to Long::class,
            Types.BINARY to Array<Byte>::class,
            Types.BIT to Boolean::class,
            Types.BLOB to Blob::class,
            Types.BOOLEAN to Boolean::class,
            Types.CHAR to String::class,
            Types.CLOB to Clob::class,
            Types.DATALINK to Any::class,
            Types.DATE to LocalDate::class,
            Types.DECIMAL to BigDecimal::class,
            Types.DISTINCT to Any::class,
            Types.DOUBLE to Double::class,
            Types.FLOAT to Double::class,
            Types.INTEGER to Int::class,
            Types.JAVA_OBJECT to Any::class,
            Types.LONGNVARCHAR to String::class,
            Types.LONGVARBINARY to Array<Byte>::class,
            Types.LONGVARCHAR to String::class,
            Types.NCHAR to String::class,
            Types.NCLOB to Clob::class,
            Types.NULL to Nothing::class,
            Types.NUMERIC to Double::class,
            Types.NVARCHAR to String::class,
            Types.OTHER to Any::class,
            Types.REAL to Float::class,
            Types.REF to Ref::class,
            Types.REF_CURSOR to Any::class,
            Types.ROWID to RowId::class,
            Types.SMALLINT to Int::class,
            Types.SQLXML to SQLXML::class,
            Types.STRUCT to Any::class,
            Types.TIME to LocalTime::class,
            Types.TIMESTAMP to LocalDateTime::class,
            Types.TIMESTAMP_WITH_TIMEZONE to LocalDateTime::class,
            Types.TIME_WITH_TIMEZONE to LocalDateTime::class,
            Types.TINYINT to Int::class,
            Types.VARBINARY to Array<Byte>::class,
            Types.VARCHAR to String::class,
        )
        expectations.forEach { (jdbcType, expected) ->
            assertEquals(
                expected,
                JdbcTypeToKotlinType.getKotlinType(RdbTypeEnum.MARIA, col("ANY_NAME", jdbcType = jdbcType)),
                "default mapping for jdbcType=$jdbcType"
            )
        }
        // the other non-branch vendors take the same path
        assertEquals(
            String::class,
            JdbcTypeToKotlinType.getKotlinType(RdbTypeEnum.DB2, col("VARCHAR", jdbcType = Types.VARCHAR))
        )
        assertEquals(
            Int::class,
            JdbcTypeToKotlinType.getKotlinType(RdbTypeEnum.CLICKHOUSE, col("INT", jdbcType = Types.INTEGER))
        )
        assertEquals(
            Boolean::class,
            JdbcTypeToKotlinType.getKotlinType(RdbTypeEnum.SQLSERVER, col("BIT", jdbcType = Types.BIT))
        )
    }

    @Test
    fun otherVendors_unsupportedJdbcTypeThrows() {
        val e = assertFailsWith<IllegalStateException> {
            JdbcTypeToKotlinType.getKotlinType(RdbTypeEnum.MARIA, col("FANCY", jdbcType = 987654))
        }
        assertTrue(e.message!!.contains("Unsupported JdbcType"), "message should name the unsupported type")
        assertTrue(e.message!!.contains("987654"))
    }
}
