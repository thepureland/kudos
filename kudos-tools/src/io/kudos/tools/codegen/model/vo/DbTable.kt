package io.kudos.tools.codegen.model.vo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * Value object for database table information.
 *
 * Represents the basic information of a database table in the code generation tool, including the table
 * name, table comment, and a flag indicating whether to generate code for it.
 *
 * Core properties:
 * - generate: flag indicating whether to generate code; controls the code generation flow
 * - tableName: database table name; used to generate the corresponding entity class and business code
 * - tableComment: database table comment; used as comment content in the generated code
 *
 * Features:
 * - Uses JavaFX property binding for reactive updates
 * - Implements Comparable for sorting by table name
 * - Provides standard getter/setter and property accessor methods
 *
 * Use cases:
 * - Displaying a list of tables in the code generation tool's UI
 * - Filtering the tables for which code should be generated
 * - Passing and storing table information
 *
 * Notes:
 * - Table names are used in generated code; ensure they conform to naming conventions
 * - Table comments become comments in the generated code; meaningful descriptions are recommended
 * - Sorting is based on the table name; tables with identical names are ordered lexicographically
 *
 * @since 1.0.0
 */
class DbTable(
    generate: Boolean,

    tableName: String,

    tableComment: String?,
): Comparable<DbTable> {

    /** Flag indicating whether to generate code; bound to the "generate" checkbox in the UI */
    private val generate = SimpleBooleanProperty()
    /** Database table name */
    private val tableName = SimpleStringProperty()
    /** Database table comment; may be null (some databases do not require comments) */
    private val tableComment = SimpleStringProperty()

    init {
        setGenerate(generate)
        setTableName(tableName)
        setTableComment(tableComment)
    }

    fun getGenerate(): Boolean = generate.get()

    fun generateProperty(): BooleanProperty = generate

    fun setGenerate(generate: Boolean) = this.generate.set(generate)

    fun getTableName(): String = tableName.get()

    fun setTableName(tableName: String) = this.tableName.set(tableName)

    fun tableNameProperty(): StringProperty = tableName

    fun getTableComment(): String? = tableComment.get()

    fun setTableComment(tableComment: String?) = this.tableComment.set(tableComment)

    fun tableCommentProperty(): StringProperty = tableComment

    /** Sort lexicographically by table name to keep the UI list stable */
    override fun compareTo(other: DbTable): Int = getTableName().compareTo(other.getTableName())

}