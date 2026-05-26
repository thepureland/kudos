package io.kudos.tools.main

import io.kudos.tools.codegen.fx.ui.MultiTablesCodeGenerateWizard
import io.kudos.tools.codegen.fx.ui.SingleTableCodeGenerateWizard
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.stage.Stage

/**
 * Code generator main entry point (JavaFX application).
 *
 * On startup it shows a simple selection window with two buttons, "Single Table" and "Multi Table",
 * which open [SingleTableCodeGenerateWizard] and [MultiTablesCodeGenerateWizard] respectively. After
 * clicking, this window closes so the child wizard owns the stage.
 *
 * @author K
 * @since 1.0.0
 */
class CodeGeneraterMain : Application() {

    /**
     * JavaFX application entry: builds the buttons and HBox, then puts them in a Scene.
     *
     * @param primaryStage the main stage provided by JavaFX
     * @author K
     * @since 1.0.0
     */
    override fun start(primaryStage: Stage) {

        val singleTableButton = Button("Single Table").apply {
            setOnAction {
                SingleTableCodeGenerateWizard().start(Stage())
                primaryStage.close()
            }
        }

        val multiTableButton = Button("Multi Table").apply {
            setOnAction {
                MultiTablesCodeGenerateWizard().start(Stage())
                primaryStage.close()
            }
        }

        val root = HBox(30.0, singleTableButton, multiTableButton).apply {
            alignment = Pos.CENTER
        }

        primaryStage.apply {
            title = "Kudos Code Generator"
            scene = Scene(root, 300.0, 100.0)
            show()
        }
    }

}

/**
 * Command-line / IDE startup entry: delegates to [Application.launch], which lets the JavaFX framework reflectively call back into [CodeGeneraterMain.start].
 *
 * @author K
 * @since 1.0.0
 */
fun main() {
    Application.launch(CodeGeneraterMain::class.java)
}
