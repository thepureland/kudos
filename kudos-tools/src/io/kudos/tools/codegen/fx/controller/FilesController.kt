package io.kudos.tools.codegen.fx.controller

import io.kudos.tools.codegen.core.CodeGenerator
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.core.TemplatePathProcessor
import io.kudos.tools.codegen.core.TemplateReader
import io.kudos.tools.codegen.model.vo.GenFile
import io.kudos.tools.codegen.service.CodeGenFileService
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

/**
 * 生成的文件选择界面JavaFx控制器
 *
 * @author K
 * @since 1.0.0
 */
class FilesController : Initializable {

    @FXML
    lateinit var fileTable: TableView<GenFile>

    private lateinit var templateModel: Map<String, Any?>

    @FXML
    private lateinit var selectEntityRelativeFilesCheckBox: CheckBox

    val selectEntityRelativeFilesProperty = SimpleBooleanProperty()

    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // 双向绑定
        selectEntityRelativeFilesCheckBox.selectedProperty().bindBidirectional(selectEntityRelativeFilesProperty)
    }

    fun readFiles() {
        val genFiles = TemplatePathProcessor.readPaths(true)
        val codeGenFiles = CodeGenFileService.read()
        genFiles.forEach {
            it.setGenerate(codeGenFiles.contains(it.getFilename()))
        }
        fileTable.items = FXCollections.observableArrayList(genFiles)
    }

    @FXML
    @Suppress
    fun generate() {
        val filePathModel = createFilePathModel()
        if (filePathModel.isEmpty()) {
            Alert(Alert.AlertType.ERROR, "未选择任何文件！").show()
            return
        }

        try {
            templateModel = CodeGeneratorContext.templateModelCreator.create()
            CodeGenerator(templateModel, filePathModel).generate()
            Alert(
                Alert.AlertType.INFORMATION, "生成成功，请查看目录：${CodeGeneratorContext.config.getCodeLoaction()}".trimIndent()
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Alert(Alert.AlertType.ERROR, "生成失败！").show()
        }
    }

    @FXML
    fun generateAll() {
        selectEntityRelativeFilesProperty.value = false
        fileTable.items.forEach { it.setGenerate(true) }
        generate()
    }

    private fun createFilePathModel(): List<GenFile> {
        val genFiles = mutableListOf<GenFile>()
        fileTable.items.filter { it.getGenerate() }.forEach {
            genFiles.add(it)
        }
        return genFiles
    }

    @FXML
    fun select(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        fileTable.items.forEach { it.setGenerate(selected) }
    }

    @FXML
    fun selectEntityRelativeFiles(e: Event?) {
        if (selectEntityRelativeFilesProperty.value) {
            fileTable.items.forEach {
                val notEntityRelative = !isEntityRelative(it.templateFileRelativePath)
                        && !isEntityRelative(TemplateReader().read(it.templateFileRelativePath).toString())
                it.setGenerate(!notEntityRelative)
            }
        } else {
            fileTable.items.forEach { it.setGenerate(false) }
        }
    }

    private fun isEntityRelative(content : String) : Boolean = content.contains($$"${entityName}")

}