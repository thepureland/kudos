package io.kudos.ability.data.rdb.jdbc.metadata

import io.kudos.base.lang.string.underscoreToHump
import java.sql.Types
import kotlin.reflect.KClass

/**
 * Column information for a relational database table.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
class Column {

    /** Column name. */
    lateinit var name: String

    /** Comment. */
    var comment: String? = null

    /** JDBC type. */
    var jdbcType: Int = Types.VARCHAR

    /** JDBC type name. */
    lateinit var jdbcTypeName: String

    /** Kotlin class corresponding to the JDBC type. */
    lateinit var kotlinType: KClass<*>

    /** Length. */
    var length: Int? = null

    /** Number of decimal digits. */
    var decimalDigits: Int? = null

    /** Default value. */
    var defaultValue: String? = null

    /** Whether the column is nullable. */
    var nullable: Boolean = true

    /** Whether the column is a primary key. */
    var primaryKey: Boolean = false

    /** Whether the column is a foreign key. */
    var foreignKey: Boolean = false

    /** Whether the column is indexed. */
    var indexed: Boolean = false

    /** Whether the column is unique. */
    var unique: Boolean = false

    /** Whether the column is a dictionary code. */
    var dictCode: Boolean = false

    /** Whether the column is auto-incrementing. */
    var autoIncrement: String? = null

    /**
     * Returns the simpleName of [kotlinType]. The simpleName of anonymous / local classes is null,
     * in which case this throws. Commonly used in code-generation scenarios to build Kotlin source strings.
     */
    fun getKotlinTypeName(): String = requireNotNull(kotlinType.simpleName) { "kotlinType.simpleName is null" }

    /**
     * Converts the snake_case [name] to camelCase. Commonly used in code-generation scenarios to
     * build Kotlin field / property names.
     */
    fun getColumnHumpName(): String = name.underscoreToHump()

}