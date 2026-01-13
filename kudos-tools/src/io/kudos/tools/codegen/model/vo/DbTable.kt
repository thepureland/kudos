package io.kudos.tools.codegen.model.vo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * 数据库表信息值对象
 * 
 * 用于代码生成工具中表示数据库表的基本信息，包括表名、表注释和是否生成代码的标识。
 * 
 * 核心属性：
 * - generate：是否生成代码的标识，用于控制代码生成流程
 * - tableName：数据库表名，用于生成对应的实体类和业务代码
 * - tableComment：数据库表注释，用于生成代码中的注释信息
 * 
 * 特性：
 * - 使用JavaFX属性绑定，支持响应式更新
 * - 实现Comparable接口，支持按表名排序
 * - 提供标准的getter/setter和属性访问方法
 * 
 * 使用场景：
 * - 代码生成工具的UI界面中展示表列表
 * - 代码生成流程中筛选需要生成代码的表
 * - 表信息的传递和存储
 * 
 * 注意事项：
 * - 表名用于生成代码，应确保表名符合命名规范
 * - 表注释会作为生成代码的注释，建议填写有意义的描述
 * - 排序基于表名，表名相同的表会按字典序排列
 * 
 * @since 1.0.0
 */
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