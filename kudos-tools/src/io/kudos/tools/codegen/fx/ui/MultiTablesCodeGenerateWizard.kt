package io.kudos.tools.codegen.fx.ui

import io.kudos.ability.ui.javafx.controls.wizard.LinearWizardFlow
import io.kudos.ability.ui.javafx.controls.wizard.Wizard
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.core.TemplateModelCreator
import io.kudos.tools.codegen.fx.controller.BatchGenerationController
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
 * 多表代码生成器向导，用户可继承此类来提供自定义的TemplateModelCreator
 *
 * @author K
 * @since 1.0.0
 */
open class MultiTablesCodeGenerateWizard : Application() {

    /**
     * 得到模板数据模型创建者
     * 开发者可通过继承CodeGenerateWizard并重写该方法来提供自定义的TemplateModelCreator,
     * 以些来达到模板和填充模板的数据可完全自定义的目的
     */
    open fun getTemplateModelCreator(): TemplateModelCreator {
        return TemplateModelCreator()
    }

    override fun start(stage: Stage) {
        val wizard = Wizard("多表代码生成器")
        CodeGeneratorContext.templateModelCreator = getTemplateModelCreator()

        // --- page 1: config page
        var fxmlLoader = FXMLLoader()
        val databasePanel = fxmlLoader.load<Parent>(javaClass.getResourceAsStream("/fxml/config.fxml"))
        val configController = fxmlLoader.getController<ConfigController>()
        val page1 = object : Wizard.WizardPane() {
            override fun onExitingPage(wizard: Wizard?) { //wizard的bug: 从page3回到page2会执行该方法
                try {
                    configController.canGoOn()
                    configController.storeConfig()
                } catch (e: Exception) {
                    e.printStackTrace()
                    Alert(Alert.AlertType.ERROR, e.message).show()
                }
            }
        }
        page1.headerText = "请配置以下信息："
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
        page2.headerText = "请选择表："
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