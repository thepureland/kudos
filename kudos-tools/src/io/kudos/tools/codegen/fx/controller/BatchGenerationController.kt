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
import java.util.*

class BatchGenerationController : Initializable {

    private lateinit var config: Config

    @FXML
    lateinit var entityTable: TableView<DbTable>

    private lateinit var tableMap: Map<String, String?>

    @FXML
    private lateinit var onlyEntityRelativeFilesCheckBox: CheckBox

    private val onlyEntityRelativeFilesProperty = SimpleBooleanProperty()

    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        // 双向绑定
        onlyEntityRelativeFilesCheckBox.selectedProperty().bindBidirectional(onlyEntityRelativeFilesProperty)
        onlyEntityRelativeFilesProperty.set(true)
    }

    fun initTable() {
        tableMap = CodeGenObjectService.readTables()
        entityTable.items = FXCollections.observableArrayList(tableMap.map { DbTable(false, it.key, it.value) })
    }

    @FXML
    fun select(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        entityTable.items.forEach { it.setGenerate(selected) }
    }

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

            // 再生成表相关的文件
            selectTables.forEach {
                CodeGeneratorContext.tableName = it.getTableName()
                CodeGeneratorContext.tableComment = it.getTableComment() ?: ""
                CodeGeneratorContext.columns = CodeGenColumnService.readColumns(it.getTableName())
                val templateModel = CodeGeneratorContext.templateModelCreator.create()
                val allFilePaths = TemplatePathProcessor.readPaths(true)
                val nonEntityRelativeFilePathnames = nonEntityRelativeFilePaths.map { f -> f.templateFileRelativePath }
                val entityRelativeFilePaths = allFilePaths.filter { p -> p.templateFileRelativePath !in nonEntityRelativeFilePathnames }
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

    fun setConfig(config: Config) {
        this.config = config
    }

    fun getConfig() = config

}