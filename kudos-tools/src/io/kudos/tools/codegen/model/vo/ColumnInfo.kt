package io.kudos.tools.codegen.model.vo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * 数据库列信息值对象
 * 
 * 用于代码生成工具中表示数据库列的基本信息和代码生成配置。
 * 
 * 核心属性：
 * - name：列名，对应数据库表的列名
 * - origComment：原始列注释，从数据库元数据中获取
 * - customComment：自定义列注释，可在代码生成时覆盖原始注释
 * 
 * 代码生成配置：
 * - searchItem：是否在搜索表单中生成该字段
 * - listItem：是否在列表页面中显示该字段
 * - editItem：是否在编辑表单中生成该字段
 * - detailItem：是否在详情页面中显示该字段
 * - cacheItem：是否在缓存对象中包含该字段
 * 
 * 特性：
 * - 使用JavaFX属性绑定，支持响应式更新
 * - 提供标准的getter/setter和属性访问方法
 * - 支持自定义注释覆盖原始注释
 * 
 * 使用场景：
 * - 代码生成工具的UI界面中配置列信息
 * - 代码生成流程中根据配置生成对应的字段
 * - 列信息的传递和存储
 * 
 * 注意事项：
 * - 列名用于生成代码，应确保列名符合命名规范
 * - 如果设置了customComment，会优先使用自定义注释
 * - 代码生成配置会影响最终生成的代码结构
 * 
 * @since 1.0.0
 */
class ColumnInfo {

    private var name: String? = null
    private var origComment: String? = null
    private val customComment = SimpleStringProperty()
    private val searchItem = SimpleBooleanProperty()
    private val listItem = SimpleBooleanProperty()
    private val editItem = SimpleBooleanProperty()
    private val detailItem = SimpleBooleanProperty()
    private val cacheItem = SimpleBooleanProperty()

    fun getName(): String? = name

    fun setName(name: String?) {
        this.name = name
    }

    fun getSearchItem(): Boolean = searchItem.get()

    fun searchItemProperty(): BooleanProperty = searchItem

    fun setSearchItem(searchItem: Boolean) = this.searchItem.set(searchItem)

    fun getListItem(): Boolean = listItem.get()

    fun listItemProperty(): BooleanProperty = listItem

    fun setListItem(listItem: Boolean) = this.listItem.set(listItem)

    fun getEditItem(): Boolean = editItem.get()

    fun editItemProperty(): BooleanProperty = editItem

    fun setEditItem(editItem: Boolean) = this.editItem.set(editItem)

    fun getDetailItem(): Boolean = detailItem.get()

    fun detailItemProperty(): BooleanProperty = detailItem

    fun setDetailItem(detailItem: Boolean) = this.detailItem.set(detailItem)

    fun getCacheItem(): Boolean = cacheItem.get()

    fun cacheItemProperty(): BooleanProperty = cacheItem

    fun setCacheItem(cacheItem: Boolean) = this.cacheItem.set(cacheItem)

    fun getCustomComment(): String? = customComment.get()

    fun customCommentProperty(): StringProperty = customComment

    fun setCustomComment(customComment: String?) = this.customComment.set(customComment)

    fun getOrigComment(): String? = origComment

    fun setOrigComment(origComment: String?) {
        this.origComment = origComment
    }

    fun getComment(): String? =
        if (!getCustomComment().isNullOrBlank()) getCustomComment() else origComment

    fun getColumn(): String? = name
}