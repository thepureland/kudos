package io.kudos.tools.codegen.model.vo

import javafx.beans.property.BooleanProperty
import javafx.beans.property.SimpleBooleanProperty
import javafx.beans.property.SimpleStringProperty
import javafx.beans.property.StringProperty

/**
 * Value object for database column information.
 *
 * Represents the basic info and code-generation configuration of a database column in the code generation tool.
 *
 * Core properties:
 * - name: column name, matching the database table column name
 * - origComment: original column comment, obtained from the database metadata
 * - customComment: custom column comment, can override the original comment at generation time
 *
 * Code generation configuration:
 * - searchItem: whether to generate this field in the search form
 * - listItem: whether to display this field on the list page
 * - editItem: whether to generate this field in the edit form
 * - detailItem: whether to display this field on the detail page
 * - cacheItem: whether to include this field in the cache object
 *
 * Features:
 * - Uses JavaFX property binding for reactive updates
 * - Provides standard getter/setter and property accessor methods
 * - Supports overriding the original comment with a custom comment
 *
 * Use cases:
 * - Configuring column info in the code generation tool's UI
 * - Generating corresponding fields in the code generation flow based on configuration
 * - Passing and storing column information
 *
 * Notes:
 * - Column names are used in generated code; ensure they conform to naming conventions
 * - When customComment is set, the custom comment takes precedence
 * - Code generation configuration affects the structure of the final generated code
 *
 * @since 1.0.0
 */
class ColumnInfo {

    /**
     * Column name (database column name); not held as a JavaFX property because the column name does not
     * need UI binding during code generation.
     */
    private var name: String? = null
    /**
     * Original database column comment, read from metadata; for read-only display only.
     */
    private var origComment: String? = null
    /** Custom comment entered by the user in the UI; takes precedence over [origComment] as the final comment value */
    private val customComment = SimpleStringProperty()
    /** Whether to generate this field in the search form; corresponds to a checkbox in the UI */
    private val searchItem = SimpleBooleanProperty()
    /** Whether to display this field on the list page */
    private val listItem = SimpleBooleanProperty()
    /** Whether to generate this field in the edit form */
    private val editItem = SimpleBooleanProperty()
    /** Whether to display this field on the detail page */
    private val detailItem = SimpleBooleanProperty()
    /** Whether to include this field in the cache object */
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
     * Returns the column comment used by code generation: the custom comment when non-blank, otherwise the
     * original database comment as a fallback.
     *
     * @return custom or original column comment; null when both are empty
     * @author K
     * @since 1.0.0
     */
    fun getComment(): String? =
        if (!getCustomComment().isNullOrBlank()) getCustomComment() else origComment

    /**
     * Returns the column name. Semantically identical to [getName]; naming is just closer to "database column".
     *
     * @return column name
     * @author K
     * @since 1.0.0
     */
    fun getColumn(): String? = name
}