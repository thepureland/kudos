package io.kudos.tools.codegen.fx.controller

import io.kudos.ability.ui.javafx.controls.AutoCompleteComboBoxListener
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.model.vo.ColumnInfo
import io.kudos.tools.codegen.model.vo.Config
import io.kudos.tools.codegen.service.CodeGenColumnService
import io.kudos.tools.codegen.service.CodeGenObjectService
import javafx.application.Platform
import javafx.beans.value.ObservableValue
import javafx.collections.FXCollections
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.CheckBox
import javafx.scene.control.ComboBox
import javafx.scene.control.TableView
import javafx.scene.control.TextField
import java.net.URL
import java.util.ResourceBundle
import kotlin.concurrent.thread

/**
 * JavaFX controller for the database table column-info UI.
 *
 * @author K
 * @since 1.0.0
 */
class ColumnsController : Initializable {

    /** Table-name combo box, supports fuzzy matching (enhanced by [AutoCompleteComboBoxListener]) */
    @FXML
    lateinit var tableComboBox: ComboBox<Any>

    /** Comment field of the currently selected table; auto-filled from the combo box selection */
    @FXML
    lateinit var tableCommentTextField: TextField

    /** Column-info table view, bound to all [ColumnInfo] entries of the current table */
    @FXML
    lateinit var columnTable: TableView<ColumnInfo>

    /** "Detail-item select-all" checkbox; checking it batch-enables detailItem on every column */
    @FXML
    lateinit var detailCheckBox: CheckBox

    /** [Config] carried over from the previous step; used to forward info like "code output location" downstream */
    private lateinit var config: Config
    /** Mapping of database table name to table comment, loaded once when initializing the combo box */
    private var tableMap: Map<String, String?>? = null

    /**
     * JavaFX callback after the FXML is loaded; only binds events here
     * (table data is loaded later, after [setConfig] is invoked).
     *
     * @param location FXML location (unused)
     * @param resources i18n resources (unused)
     * @author K
     * @since 1.0.0
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        bindEvents()
    }

    /**
     * Loads all table names and switches the "select table" event to asynchronously load column info.
     *
     * Key points:
     * 1. The wizard has a known bug where "going back from page3 to page2 fires page1.onExitingPage",
     *    so this method may be called repeatedly. We back up the current columns/tableComment first
     *    to avoid wiping state on every reinit.
     * 2. Column loading runs on a separate thread because column-metadata queries for large tables
     *    can be slow, to avoid blocking the JavaFX UI thread.
     * 3. The listener is only attached when the combo box has no items yet — checked via
     *    `items.isEmpty()` to prevent piling up listeners across repeated initialize calls.
     *
     * @author K
     * @since 1.0.0
     */
    private fun initTableComboBox() {
        // Workaround for wizard bug: going back from page3 to page2 invokes page1's onExitingPage
        val columnList = columns
        val tableComment = tableComment
        val firstInit = tableComboBox.items.isEmpty()
        tableMap = CodeGenObjectService.readTables()
        tableComboBox.items = FXCollections.observableArrayList(requireNotNull(tableMap) { "tableMap is null" }.keys.toSortedSet())
        AutoCompleteComboBoxListener<Any>(tableComboBox)
        if (firstInit) {
            tableComboBox.editor.textProperty()
                .addListener { _: ObservableValue<out String?>?, _: String?, newValue: String? ->
                    tableCommentTextField.clear()
                    val tblMap = tableMap
                    if (newValue != null && tblMap?.containsKey(newValue) == true) {
                        tableCommentTextField.text = tblMap[newValue]
                        CodeGeneratorContext.tableName = newValue
                        thread {
                            val columns = CodeGenColumnService.readColumns(CodeGeneratorContext.tableName)
                            Platform.runLater { columnTable.items = FXCollections.observableArrayList(columns) }
                            if (columnTable.items.all { it.getDetailItem() }) {
                                detailCheckBox.selectedProperty().value = true
                            }
                        }
                    }
                }
        }
        if (columnList.isNotEmpty()) {
            tableComboBox.selectionModel.select(table)
            tableCommentTextField.text = tableComment
            columnTable.items = FXCollections.observableArrayList(columnList)
        }
    }

    /**
     * Reserved event-binding placeholder. No events currently need explicit wiring in this controller —
     * the FXML already declares all control callbacks.
     */
    private fun bindEvents() {}

    /**
     * Callback for the "detail-item select-all" checkbox: batch-applies the checkbox's selected state
     * to [ColumnInfo.setDetailItem] on every column.
     *
     * @param e the source event, target is always [detailCheckBox]
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun selectDetailItems(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        columnTable.items.forEach { it.setDetailItem(selected) }
    }

    /**
     * Receives the [Config] passed from the previous step and triggers initialization of the table combo box.
     *
     * @param config configuration gathered in the previous step
     * @author K
     * @since 1.0.0
     */
    fun setConfig(config: Config) {
        this.config = config
        initTableComboBox()
    }

    /** @return the currently held [Config] */
    fun getConfig(): Config = config

    /** Currently selected table name; null when nothing is selected */
    val table: String?
        get() = tableComboBox.selectionModel.selectedItem?.toString()

    /** Comment of the currently selected table (trimmed) */
    val tableComment: String
        get() = tableCommentTextField.text?.trim() ?: ""

    /** Column-info list of the current table, including the user's UI selections */
    val columns: List<ColumnInfo>
        get() = columnTable.items

}