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
 * JavaFX controller for the generated-file selection UI.
 *
 * @author K
 * @since 1.0.0
 */
class FilesController : Initializable {

    /** TableView listing files to be generated, bound to a list of [GenFile] */
    @FXML
    lateinit var fileTable: TableView<GenFile>

    /** Template fill model used in the current generation pass; created on demand on first generate */
    private lateinit var templateModel: Map<String, Any?>

    /** "Only select entity-related files" checkbox, to avoid accidentally overwriting unrelated files */
    @FXML
    private lateinit var selectEntityRelativeFilesCheckBox: CheckBox

    /** Backing property for [selectEntityRelativeFilesCheckBox]; lets external code (the wizard) toggle it programmatically */
    val selectEntityRelativeFilesProperty = SimpleBooleanProperty()

    /**
     * Binds the "only entity-related files" checkbox to its backing property.
     * The default check state is set explicitly by the wizard on page entry, to avoid this controller
     * and the upper-level UI each defining conflicting defaults.
     *
     * @author K
     * @since 1.0.0
     */
    override fun initialize(location: URL?, resources: ResourceBundle?) {
        // bidirectional binding
        selectEntityRelativeFilesCheckBox.selectedProperty().bindBidirectional(selectEntityRelativeFilesProperty)
    }

    /**
     * Reads every file that will be generated this run and auto-selects ones matching previously-generated
     * file names. On repeat generations this defaults to refreshing only files that already exist,
     * reducing accidental overwrites.
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
     * Callback for the "Generate" button; invokes [CodeGenerator] with the selected files and
     * shows an Alert with the outcome. Early-returns with an error alert when nothing is selected,
     * to avoid a no-op run.
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    @Suppress
    fun generate() {
        val filePathModel = createFilePathModel()
        if (filePathModel.isEmpty()) {
            Alert(Alert.AlertType.ERROR, "No files selected!").show()
            return
        }

        try {
            templateModel = CodeGeneratorContext.templateModelCreator.create()
            CodeGenerator(templateModel, filePathModel).generate()
            Alert(
                Alert.AlertType.INFORMATION, "Generation succeeded, see directory: ${CodeGeneratorContext.config.getCodeLoaction()}".trimIndent()
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Alert(Alert.AlertType.ERROR, "Generation failed!").show()
        }
    }

    /**
     * Callback for the "Generate All" button; forces the "only entity-related" toggle off,
     * checks every file, and runs generation. The toggle is disabled so that a subsequent
     * [generate] call does not have its selection inverted by that filter.
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
     * Extracts the entries marked for generation in the UI table to pass into [CodeGenerator].
     *
     * @return list of files whose "generate" flag is checked
     * @author K
     * @since 1.0.0
     */
    private fun createFilePathModel(): List<GenFile> = fileTable.items.filter { it.getGenerate() }

    /**
     * Header "select-all / select-none" checkbox callback; batch-syncs every [GenFile.setGenerate].
     *
     * @param e source event
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun select(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        fileTable.items.forEach { it.setGenerate(selected) }
    }

    /**
     * Callback for the "only select entity-related files" checkbox; may also be triggered
     * programmatically when the wizard enters this page.
     * Checked: only files whose template path or body contains `${entityName}` are marked for generation.
     * Unchecked: sets all generate flags to false (the user has to pick manually).
     *
     * @param e source event; may be null when invoked programmatically
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
     * Tests whether a string contains the `${entityName}` placeholder, used as the "entity-related"
     * criterion. Equivalent to [TemplatePathProcessor.isEntityRelative] but scoped locally to avoid
     * cross-module circular dependency.
     *
     * @param content string to test
     * @return true if the text references the entity-name placeholder
     * @author K
     * @since 1.0.0
     */
    private fun isEntityRelative(content : String) : Boolean = content.contains($$"${entityName}")

}