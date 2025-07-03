package io.kudos.tools.codegen.model.vo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

class DbTable(
    generate: Boolean,

    tableName: String,

    tableComment: String?,
): Comparable<DbTable> {

    private val generate = SimpleBooleanProperty()
    private val tableName = SimpleStringProperty()
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

    override fun compareTo(other: DbTable): Int = getTableName().compareTo(other.getTableName())

}