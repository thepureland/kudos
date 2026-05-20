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
 * 数据库表的列信息界面JavaFx控制器
 *
 * @author K
 * @since 1.0.0
 */
class ColumnsController : Initializable {

    /** 表名选择下拉框，支持模糊匹配（由 [AutoCompleteComboBoxListener] 增强） */
    @FXML
    lateinit var tableComboBox: ComboBox<Any>

    /** 当前选中表的注释文本框，根据下拉选择自动填充 */
    @FXML
    lateinit var tableCommentTextField: TextField

    /** 列信息表格，绑定到当前表的所有 [ColumnInfo] */
    @FXML
    lateinit var columnTable: TableView<ColumnInfo>

    /** "详情项全选"复选框；勾选后批量打开所有列的 detailItem */
    @FXML
    lateinit var detailCheckBox: CheckBox

    /** 从上一步带过来的 [Config]，用于把"代码输出位置"等信息透传到下一步 */
    private lateinit var config: Config
    /** 数据库表名 → 表注释的映射，初始化下拉框时一次性读取 */
    private var tableMap: Map<String, String?>? = null

    /**
     * JavaFX 在加载 FXML 后回调；此处仅绑定事件（表数据要等 [setConfig] 注入后再加载）。
     *
     * @param location FXML 位置（未使用）
     * @param resources i18n 资源（未使用）
     * @author K
     * @since 1.0.0
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        bindEvents()
    }

    /**
     * 加载所有表名，并把"选表"事件改成异步加载列信息。
     *
     * 关键点：
     * 1. wizard 有"从 page3 回到 page2 会执行 page1.onExitingPage"的 bug，
     *    导致本方法可能被反复调用——所以一开始先把当前 columns/tableComment 备份，避免反复初始化清空状态。
     * 2. 列加载放新线程是因为大表的 column 元数据查询可能慢，避免阻塞 JavaFX UI 线程。
     * 3. 仅当下拉框尚未挂 listener 时才挂——通过判断 `items.isEmpty()` 防止反复 initialize 累积 listener。
     *
     * @author K
     * @since 1.0.0
     */
    private fun initTableComboBox() {
        //解决wizard的bug: 从page3回到page2会执行page1的onExitingPage方法
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
     * 预留的事件绑定占位。当前没有需要 controller 显式连接的事件——FXML 已经声明了所有控件回调。
     */
    private fun bindEvents() {}

    /**
     * "详情项全选"复选框的回调：把所有列的 [ColumnInfo.setDetailItem] 批量改为该复选框的勾选状态。
     *
     * @param e 触发事件，目标固定为 [detailCheckBox]
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun selectDetailItems(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        columnTable.items.forEach { it.setDetailItem(selected) }
    }

    /**
     * 接收上一步传过来的 [Config] 并触发表下拉框初始化。
     *
     * @param config 上一步收集到的配置
     * @author K
     * @since 1.0.0
     */
    fun setConfig(config: Config) {
        this.config = config
        initTableComboBox()
    }

    /** @return 当前持有的 [Config] */
    fun getConfig(): Config = config

    /** 当前选中的表名；未选时返回 null */
    val table: String?
        get() = tableComboBox.selectionModel.selectedItem?.toString()

    /** 当前选中表的注释（已 trim） */
    val tableComment: String
        get() = tableCommentTextField.text?.trim() ?: ""

    /** 当前表的列信息列表，包含用户在 UI 上的勾选状态 */
    val columns: List<ColumnInfo>
        get() = columnTable.items

}