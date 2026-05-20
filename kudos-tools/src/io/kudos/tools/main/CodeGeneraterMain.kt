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
 * 代码生成器主入口（JavaFX 应用）。
 *
 * 启动后弹一个简单选择窗：「单表」/「多表」两个按钮分别打开 [SingleTableCodeGenerateWizard]
 * / [MultiTablesCodeGenerateWizard]，点击后关闭本窗，让子向导独占舞台。
 *
 * @author K
 * @since 1.0.0
 */
class CodeGeneraterMain : Application() {

    /**
     * JavaFX 应用入口：构造按钮 + HBox，置入 Scene。
     *
     * @param primaryStage JavaFX 提供的主舞台
     * @author K
     * @since 1.0.0
     */
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

/**
 * 命令行 / IDE 启动入口：委托给 [Application.launch]，由 JavaFX 框架反射回调 [CodeGeneraterMain.start]。
 *
 * @author K
 * @since 1.0.0
 */
fun main() {
    Application.launch(CodeGeneraterMain::class.java)
}
