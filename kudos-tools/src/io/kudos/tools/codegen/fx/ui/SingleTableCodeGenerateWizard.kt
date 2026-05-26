package io.kudos.tools.codegen.fx.ui

import io.kudos.ability.ui.javafx.controls.wizard.LinearWizardFlow
import io.kudos.ability.ui.javafx.controls.wizard.Wizard
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.core.TemplateModelCreator
import io.kudos.tools.codegen.fx.controller.ColumnsController
import io.kudos.tools.codegen.fx.controller.ConfigController
import io.kudos.tools.codegen.fx.controller.FilesController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.Button
import javafx.scene.control.ButtonType
import javafx.stage.Stage

/**
 * Single-table code generator wizard; subclass this to provide a custom TemplateModelCreator.
 *
 * @author K
 * @since 1.0.0
 */
open class SingleTableCodeGenerateWizard : Application() {

    /**
     * Returns the template data-model creator.
     * Developers can subclass CodeGenerateWizard and override this method to supply a custom
     * TemplateModelCreator, allowing both the template and the data filled into it to be fully customized.
     */
    open fun getTemplateModelCreator(): TemplateModelCreator = TemplateModelCreator()

    /**
     * JavaFX application entry point; builds and shows the three-step wizard "config → pick columns → pick files".
     *
     * State is passed between pages via the [CodeGeneratorContext] singleton: page1 writes config / dataSource,
     * page2 stores tableName + columns + config into the context, page3 reads the context to prepare the file list.
     *
     * Note: the retained comment about the `bug: going back from page3 to page2 fires onExitingPage`
     * refers to a known issue in the legacy Wizard control; the surrounding try/catch is a workaround —
     * removing it causes back navigation to throw.
     *
     * @param stage primary JavaFX stage
     * @author K
     * @since 1.0.0
     */
    override fun start(stage: Stage) {
        val wizard = Wizard("Single-table Code Generator")
        CodeGeneratorContext.templateModelCreator = getTemplateModelCreator()

        // config page
        var fxmlLoader = FXMLLoader()
        val databasePanel = fxmlLoader.load<Parent>(javaClass.getResourceAsStream("/fxml/config.fxml"))
        val configController = fxmlLoader.getController<ConfigController>()

        // --- page 2
        fxmlLoader = FXMLLoader()
        val columnsPanel = fxmlLoader.load<Parent>(javaClass.getResourceAsStream("/fxml/columns.fxml"))
        val columnsController = fxmlLoader.getController<ColumnsController>()

        // --- page 1
        val page1 = object : Wizard.WizardPane() {
            override fun onExitingPage(wizard: Wizard?) { // wizard bug: going back from page3 to page2 invokes this method
                try {
                    configController.canGoOn()
                    configController.storeConfig()
                    val conf = configController.config
                    columnsController.setConfig(conf)
                } catch (e: Exception) {
                    e.printStackTrace()
                    Alert(Alert.AlertType.ERROR, e.message).show()
                }
            }
        }
        page1.headerText = "Please configure the following:"
        page1.content = databasePanel

        // --- page 2
        val page2 = object : Wizard.WizardPane() {
            override fun onExitingPage(wizard: Wizard?) {
                val table = columnsController.table
                if (table == null) {
                    Alert(Alert.AlertType.ERROR, "Please select a table first!").show()
                } else {
                    CodeGeneratorContext.tableName = table
                    CodeGeneratorContext.tableComment = columnsController.tableComment
                    CodeGeneratorContext.columns = columnsController.columns
                    CodeGeneratorContext.config = columnsController.getConfig()
                }
            }

            override fun onEnteringPage(wizard: Wizard?) {
                println("entering page 2")
            }
        }
        page2.headerText = "Please customize the column info:"
        page2.content = columnsPanel

        // --- page 3
        fxmlLoader = FXMLLoader()
        val filesPanel = fxmlLoader.load<Parent>(javaClass.getResourceAsStream("/fxml/files.fxml"))
        val filesController = fxmlLoader.getController<FilesController>()
        val page3 = object : Wizard.WizardPane() {
            override fun onEnteringPage(wizard: Wizard?) {
                filesController.readFiles()
                val button = lookupButton(ButtonType.FINISH) as Button
                button.isDisable = true
                filesController.selectEntityRelativeFilesProperty.set(true)
                filesController.selectEntityRelativeFiles(null)
            }
        }
        page3.headerText = "Please select files to generate:"
        page3.content = filesPanel


        // create wizard
        wizard.setFlow(LinearWizardFlow(page1, page2, page3))

        // show wizard and wait for response
        wizard.showAndWait().ifPresent { result: ButtonType? ->
            when (result) {
                ButtonType.FINISH -> println("Wizard finished, settings: ${wizard.settings}")
                ButtonType.PREVIOUS -> println("PREVIOUS")
                ButtonType.NEXT -> println("NEXT")
            }
        }
    }

//    private fun maxScreen(stage: Stage) {
//        val primaryScreenBounds = Screen.getPrimary().visualBounds
//        stage.x = primaryScreenBounds.minX
//        stage.y = primaryScreenBounds.minY
//        stage.width = primaryScreenBounds.width
//        stage.height = primaryScreenBounds.height
//    }

}