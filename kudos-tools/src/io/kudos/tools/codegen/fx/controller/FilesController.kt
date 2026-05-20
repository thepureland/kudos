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
import java.util.ResourceBundle

/**
 * 生成的文件选择界面JavaFx控制器
 *
 * @author K
 * @since 1.0.0
 */
class FilesController : Initializable {

    /** 待生成文件列表 TableView，绑定 [GenFile] 列表 */
    @FXML
    lateinit var fileTable: TableView<GenFile>

    /** 当次生成使用的模板填充模型；首次 generate 时按需创建 */
    private lateinit var templateModel: Map<String, Any?>

    /** "仅勾选表相关文件"复选框，避免误触发覆盖无关文件 */
    @FXML
    private lateinit var selectEntityRelativeFilesCheckBox: CheckBox

    /** [selectEntityRelativeFilesCheckBox] 的支撑属性，供外部代码（wizard）以编程方式改写 */
    val selectEntityRelativeFilesProperty = SimpleBooleanProperty()

    /**
     * 把"仅勾选表相关文件"复选框与支撑属性绑定。
     * 默认勾选状态由 wizard 在进入本页时显式赋值，避免本控制器和上层 UI 各自定义默认值冲突。
     *
     * @author K
     * @since 1.0.0
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // 双向绑定
        selectEntityRelativeFilesCheckBox.selectedProperty().bindBidirectional(selectEntityRelativeFilesProperty)
    }

    /**
     * 读取本次会生成的所有文件，按"上次生成过的同名文件"自动勾选——重复生成时默认只刷新历史已有文件，
     * 减少误覆盖。
     *
     * @author K
     * @since 1.0.0
     */
    fun readFiles() {
        val genFiles = TemplatePathProcessor.readPaths(true)
        val codeGenFiles = CodeGenFileService.read()
        genFiles.forEach {
            it.setGenerate(codeGenFiles.contains(it.getFilename()))
        }
        fileTable.items = FXCollections.observableArrayList(genFiles)
    }

    /**
     * "生成"按钮回调；按勾选项调 [CodeGenerator]，弹 Alert 反馈成败。
     * 没勾选任何文件时早退给出错误提示，避免空操作。
     *
     * @author K
     * @since 1.0.0
     */
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

    /**
     * "全部生成"按钮回调；强制取消"仅勾选表相关"开关、勾全所有文件、直接跑生成。
     * 关闭"仅表相关"是为了避免随后再 [generate] 时被该过滤逻辑反向取消勾选。
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun generateAll() {
        selectEntityRelativeFilesProperty.value = false
        fileTable.items.forEach { it.setGenerate(true) }
        generate()
    }

    /**
     * 把 UI 表格里勾选了 generate 的项摘出来，作为 [CodeGenerator] 的入参。
     *
     * @return 勾选了"生成"的文件列表
     * @author K
     * @since 1.0.0
     */
    private fun createFilePathModel(): List<GenFile> = fileTable.items.filter { it.getGenerate() }

    /**
     * 表头"全选/全不选"复选框回调；批量同步所有 [GenFile.setGenerate]。
     *
     * @param e 触发事件
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun select(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        fileTable.items.forEach { it.setGenerate(selected) }
    }

    /**
     * "仅勾选表相关文件"复选框回调，也可在 wizard 进入页面时编程触发。
     * 勾选：只把模板路径或正文里出现 `${entityName}` 的文件标记为生成；
     * 取消勾选：把所有 generate 设为 false（让用户自行手选）。
     *
     * @param e 触发事件，编程调用时可传 null
     * @author K
     * @since 1.0.0
     */
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

    /**
     * 判断字符串中是否包含 `${entityName}` 占位符，作为"实体相关"的判定条件。
     * 与 [TemplatePathProcessor.isEntityRelative] 同义但作用域独立，避免跨模块循环依赖。
     *
     * @param content 待检测的字符串
     * @return true 表示文本中引用了实体名占位符
     * @author K
     * @since 1.0.0
     */
    private fun isEntityRelative(content : String) : Boolean = content.contains($$"${entityName}")

}