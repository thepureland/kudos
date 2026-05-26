package io.kudos.tools.codegen.fx.ui

import io.kudos.ability.ui.javafx.controls.wizard.LinearWizardFlow
import io.kudos.ability.ui.javafx.controls.wizard.Wizard
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.core.TemplateModelCreator
import io.kudos.tools.codegen.fx.controller.BatchGenerationController
import io.kudos.tools.codegen.fx.controller.ConfigController
import javafx.application.Application
import javafx.fxml.FXMLLoader
import javafx.scene.Parent
import javafx.scene.control.Alert
import javafx.scene.control.ButtonType
import javafx.stage.Stage

/**
 * Multi-table code generator wizard; subclass this to provide a custom TemplateModelCreator.
 *
 * @author K
 * @since 1.0.0
 */
open class MultiTablesCodeGenerateWizard : Application() {

    /**
     * Returns the template data-model creator.
     * Developers can subclass CodeGenerateWizard and override this method to supply a custom
     * TemplateModelCreator, allowing both the template and the data filled into it to be fully customized.
     */
    open fun getTemplateModelCreator(): TemplateModelCreator = TemplateModelCreator()

    /**
     * JavaFX application entry point; builds the two-step "config → select tables for batch generation" wizard.
     *
     * The main difference from [SingleTableCodeGenerateWizard] is that page2 uses [BatchGenerationController]
     * to select multiple tables in one shot — there is no page3 for per-file selection because that is
     * impractical in the batch scenario.
     *
     * @param stage primary JavaFX stage
     * @author K
     * @since 1.0.0
     */
    override fun start(stage: Stage) {
        val wizard = Wizard("Multi-table Code Generator")
        CodeGeneratorContext.templateModelCreator = getTemplateModelCreator()

        // --- page 1: config page
        var fxmlLoader = FXMLLoader()
        val databasePanel = fxmlLoader.load<Parent>(javaClass.getResourceAsStream("/fxml/config.fxml"))
        val configController = fxmlLoader.getController<ConfigController>()
        val page1 = object : Wizard.WizardPane() {
            override fun onExitingPage(wizard: Wizard?) { // wizard bug: going back from page3 to page2 invokes this method
                try {
                    configController.canGoOn()
                    configController.storeConfig()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Alert(Alert.AlertType.ERROR, e.message).show()
                }
            }
        }
        page1.headerText = "Please configure the following:"
        page1.content = databasePanel


        // --- page 2：tables
        fxmlLoader = FXMLLoader()
        val tablesPanel = fxmlLoader.load<Parent>(javaClass.getResourceAsStream("/fxml/batch.fxml"))
        val tablesController = fxmlLoader.getController<BatchGenerationController>()
        val page2 = object : Wizard.WizardPane() {
            override fun onEnteringPage(wizard: Wizard?) {
                tablesController.setConfig(configController.config)
                tablesController.initTable()
                val button = lookupButton(ButtonType.FINISH)
                button.isDisable = true
            }
        }
        page2.headerText = "Please select tables:"
        page2.content = tablesPanel


        // create wizard
        wizard.setFlow(LinearWizardFlow(page1, page2))

        // show wizard and wait for response
        wizard.showAndWait().ifPresent { result: ButtonType? ->
            when (result) {
                ButtonType.FINISH -> println("Wizard finished, settings: ${wizard.settings}")
                ButtonType.PREVIOUS -> println("PREVIOUS")
                ButtonType.NEXT -> println("NEXT")
            }
        }
    }

}