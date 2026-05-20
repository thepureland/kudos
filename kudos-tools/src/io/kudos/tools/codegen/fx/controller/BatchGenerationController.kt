package io.kudos.tools.codegen.fx.controller

import io.kudos.tools.codegen.core.CodeGenerator
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.core.TemplatePathProcessor
import io.kudos.tools.codegen.model.vo.Config
import io.kudos.tools.codegen.model.vo.DbTable
import io.kudos.tools.codegen.service.CodeGenColumnService
import io.kudos.tools.codegen.service.CodeGenObjectService
import javafx.beans.property.SimpleBooleanProperty
import javafx.collections.FXCollections
import javafx.event.Event
import javafx.fxml.FXML
import javafx.fxml.Initializable
import javafx.scene.control.Alert
import javafx.scene.control.CheckBox
import javafx.scene.control.TableView
import java.net.URL
import java.util.ResourceBundle

/**
 * 多表批量生成界面的 JavaFX 控制器。
 *
 * 上一步配置完后跳到本页：列出所有表，用户勾选要生成的表与"仅生成表相关文件"开关，
 * 触发 [generate] 后依次对每张表跑 [CodeGenerator]。
 *
 * @author K
 * @since 1.0.0
 */
class BatchGenerationController : Initializable {

    /** 从配置页带过来的 [Config] */
    private lateinit var config: Config

    /** 表清单 TableView，绑定 [DbTable] 列表 */
    @FXML
    lateinit var entityTable: TableView<DbTable>

    /** 数据库表名 → 表注释，进入页面时一次性读取 */
    private lateinit var tableMap: Map<String, String?>

    /** "仅生成表相关文件"复选框 */
    @FXML
    private lateinit var onlyEntityRelativeFilesCheckBox: CheckBox

    /** [onlyEntityRelativeFilesCheckBox] 的支撑属性，便于在程序里读 boolean */
    private val onlyEntityRelativeFilesProperty = SimpleBooleanProperty()

    /**
     * FXML 加载完成回调：把"仅生成表相关文件"复选框与 [onlyEntityRelativeFilesProperty] 双向绑定，
     * 并默认勾选——批量场景下生成无关文件容易把全局基类等覆盖掉，默认排除更安全。
     *
     * @author K
     * @since 1.0.0
     */
    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        // 双向绑定
        onlyEntityRelativeFilesCheckBox.selectedProperty().bindBidirectional(onlyEntityRelativeFilesProperty)
        onlyEntityRelativeFilesProperty.set(true)
    }

    /**
     * 读取所有表名/注释并填入 [entityTable]，默认都不勾选生成。
     *
     * @author K
     * @since 1.0.0
     */
    fun initTable() {
        tableMap = CodeGenObjectService.readTables()
        entityTable.items = FXCollections.observableArrayList(tableMap.map { DbTable(false, it.key, it.value) })
    }

    /**
     * 表头"全选/全不选"复选框回调；批量改写所有 [DbTable.setGenerate]。
     *
     * @param e 触发事件
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun select(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        entityTable.items.forEach { it.setGenerate(selected) }
    }

    /**
     * "生成"按钮回调。
     *
     * 流程：
     * 1. 校验有勾选；
     * 2. 先用基础模型生成"表无关"文件（若用户没勾选"仅表相关"）；
     * 3. 再对每张选中的表把上下文切换好，跑实体相关模板；
     * 4. 弹 Alert 反馈成败。
     *
     * 实体相关文件集合是用「全部文件 - 表无关文件」差集得到的，避免重复生成。
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun generate() {
        val selectTables = entityTable.items.filter { it.getGenerate() }
        if (selectTables.isEmpty()) {
            Alert(Alert.AlertType.ERROR, "未选择任何表！").show()
            return
        }

        try {
            CodeGeneratorContext.config = config

            // 先生成表无关的文件
            val nonEntityRelativeFilePaths = TemplatePathProcessor.readPaths(false)
            if (!onlyEntityRelativeFilesProperty.get()) {
                val templateBaseModel = CodeGeneratorContext.templateModelCreator.createBaseModel()
                CodeGenerator(templateBaseModel, nonEntityRelativeFilePaths).generate(false)
            }

            // 预计算一次：表无关模板路径，作为 Set 给后面循环内做 O(1) 差集
            val nonEntityRelativeFilePathnames = nonEntityRelativeFilePaths.mapTo(mutableSetOf()) { it.templateFileRelativePath }

            // 再生成表相关的文件
            selectTables.forEach {
                CodeGeneratorContext.tableName = it.getTableName()
                CodeGeneratorContext.tableComment = it.getTableComment() ?: ""
                CodeGeneratorContext.columns = CodeGenColumnService.readColumns(it.getTableName())
                val templateModel = CodeGeneratorContext.templateModelCreator.create()
                val entityRelativeFilePaths = TemplatePathProcessor.readPaths(true)
                    .filter { p -> p.templateFileRelativePath !in nonEntityRelativeFilePathnames }
                CodeGenerator(templateModel, entityRelativeFilePaths).generate()
            }

            Alert(
                Alert.AlertType.INFORMATION, "生成成功，请查看目录：${CodeGeneratorContext.config.getCodeLoaction()}".trimIndent()
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Alert(Alert.AlertType.ERROR, "生成失败！").show()
        }
    }

    /**
     * 从上一步注入配置。
     * @param config 配置 VO
     * @author K
     * @since 1.0.0
     */
    fun setConfig(config: Config) {
        this.config = config
    }

    /** @return 当前持有的 [Config] */
    fun getConfig() = config

}