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
 * JavaFX controller for the multi-table batch-generation screen.
 *
 * Reached after the configuration page: lists all tables, lets the user pick which tables to generate and
 * toggle "generate only table-related files", then runs [CodeGenerator] for each selected table on [generate].
 *
 * @author K
 * @since 1.0.0
 */
class BatchGenerationController : Initializable {

    /** [Config] handed off from the configuration page */
    private lateinit var config: Config

    /** Table-listing TableView, bound to a [DbTable] list */
    @FXML
    lateinit var entityTable: TableView<DbTable>

    /** DB table name -> table comment, read once when entering the page */
    private lateinit var tableMap: Map<String, String?>

    /** "Only generate table-related files" checkbox */
    @FXML
    private lateinit var onlyEntityRelativeFilesCheckBox: CheckBox

    /** Backing property for [onlyEntityRelativeFilesCheckBox]; provides a boolean for programmatic reads */
    private val onlyEntityRelativeFilesProperty = SimpleBooleanProperty()

    /**
     * FXML-loaded callback: bidirectionally binds the "only table-related files" checkbox to
     * [onlyEntityRelativeFilesProperty] and defaults it to checked — generating non-related files in batch mode
     * can easily overwrite global base classes; excluding them by default is safer.
     *
     * @author K
     * @since 1.0.0
     */
    override fun initialize(p0: URL?, p1: ResourceBundle?) {
        // Bidirectional binding
        onlyEntityRelativeFilesCheckBox.selectedProperty().bindBidirectional(onlyEntityRelativeFilesProperty)
        onlyEntityRelativeFilesProperty.set(true)
    }

    /**
     * Reads all table names/comments and fills [entityTable]; nothing is checked for generation by default.
     *
     * @author K
     * @since 1.0.0
     */
    fun initTable() {
        tableMap = CodeGenObjectService.readTables()
        entityTable.items = FXCollections.observableArrayList(tableMap.map { DbTable(false, it.key, it.value) })
    }

    /**
     * Callback for the header "select all / select none" checkbox; updates all [DbTable.setGenerate] entries.
     *
     * @param e Triggering event
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun select(e: Event) {
        val selected = (e.target as CheckBox).isSelected
        entityTable.items.forEach { it.setGenerate(selected) }
    }

    /**
     * "Generate" button callback.
     *
     * Flow:
     * 1. Validate that something is selected;
     * 2. Generate the "table-independent" files first using the base model (unless the user picked "table-related only");
     * 3. For each selected table, switch the context and run the entity-related templates;
     * 4. Show an Alert with success/failure feedback.
     *
     * The entity-related file set is computed as "all files - table-independent files" to avoid duplicate generation.
     *
     * @author K
     * @since 1.0.0
     */
    @FXML
    fun generate() {
        val selectTables = entityTable.items.filter { it.getGenerate() }
        if (selectTables.isEmpty()) {
            Alert(Alert.AlertType.ERROR, "No table selected!").show()
            return
        }

        try {
            CodeGeneratorContext.config = config

            // Generate the table-independent files first
            val nonEntityRelativeFilePaths = TemplatePathProcessor.readPaths(false)
            if (!onlyEntityRelativeFilesProperty.get()) {
                val templateBaseModel = CodeGeneratorContext.templateModelCreator.createBaseModel()
                CodeGenerator(templateBaseModel, nonEntityRelativeFilePaths).generate(false)
            }

            // Precompute once: the table-independent template paths as a Set so the loop below can do O(1) diffing
            val nonEntityRelativeFilePathnames = nonEntityRelativeFilePaths.mapTo(mutableSetOf()) { it.templateFileRelativePath }

            // Then generate the table-related files
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
                Alert.AlertType.INFORMATION, "Generation succeeded; see directory: ${CodeGeneratorContext.config.getCodeLoaction()}".trimIndent()
            ).show()
        } catch (e: Exception) {
            e.printStackTrace()
            Alert(Alert.AlertType.ERROR, "Generation failed!").show()
        }
    }

    /**
     * Injects the configuration from the previous step.
     * @param config Configuration VO
     * @author K
     * @since 1.0.0
     */
    fun setConfig(config: Config) {
        this.config = config
    }

    /** @return The currently held [Config] */
    fun getConfig() = config

}