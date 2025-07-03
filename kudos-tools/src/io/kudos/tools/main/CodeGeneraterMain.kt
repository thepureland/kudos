package io.kudos.tools.main

import io.kudos.tools.codegen.fx.ui.MultiTablesCodeGenerateWizard
import io.kudos.tools.codegen.fx.ui.SingleTableCodeGenerateWizard
import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.control.Button
import javafx.scene.layout.HBox
import javafx.stage.Stage

class CodeGeneraterMain : Application() {

    override fun start(primaryStage: Stage) {

        val singleTableButton = Button("单表").apply {
            setOnAction {
                SingleTableCodeGenerateWizard().start(Stage())
                primaryStage.close()
            }
        }

        val multiTableButton = Button("多表").apply {
            setOnAction {
                MultiTablesCodeGenerateWizard().start(Stage())
                primaryStage.close()
            }
        }

        val root = HBox(30.0, singleTableButton, multiTableButton).apply {
            alignment = Pos.CENTER
        }

        primaryStage.apply {
            title = "Kudos代码生成器"
            scene = Scene(root, 300.0, 100.0)
            show()
        }
    }

}

fun main() {
    Application.launch(CodeGeneraterMain::class.java)
}
