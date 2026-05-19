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

    /**
     * 列名（数据库列名）；不走 JavaFX 属性是因为列名在代码生成期不需要绑定 UI。
     */
    private var name: String? = null
    /**
     * 数据库原始列注释，从元数据读取，仅作只读展示。
     */
    private var origComment: String? = null
    /** 用户在 UI 中输入的自定义注释；优先级高于 [origComment]，可作为代码注释最终值 */
    private val customComment = SimpleStringProperty()
    /** 是否在搜索表单中生成该字段；对应 UI 中的复选框 */
    private val searchItem = SimpleBooleanProperty()
    /** 是否在列表页面中显示该字段 */
    private val listItem = SimpleBooleanProperty()
    /** 是否在编辑表单中生成该字段 */
    private val editItem = SimpleBooleanProperty()
    /** 是否在详情页面中显示该字段 */
    private val detailItem = SimpleBooleanProperty()
    /** 是否在缓存对象中包含该字段 */
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

    /**
     * 取最终用于代码生成的列注释：自定义注释非空则用之，否则回退到数据库原始注释。
     *
     * @return 自定义或原始列注释；都为空时返回 null
     * @author K
     * @since 1.0.0
     */
    fun getComment(): String? =
        if (!getCustomComment().isNullOrBlank()) getCustomComment() else origComment

    /**
     * 返回列名。语义上与 [getName] 一致，只是命名上更贴近"数据库列"。
     *
     * @return 列名
     * @author K
     * @since 1.0.0
     */
    fun getColumn(): String? = name
}