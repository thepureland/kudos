package io.kudos.tools.main

import io.kudos.tools.codegen.fx.ui.CodeGenerateWizard
import javafx.application.Application

fun main(args: Array<String>) {
    Application.launch(CodeGenerateWizard::class.java, *args) // 可继承CodeGenerateWizard，提供自定义的模板数据模型创建者
}