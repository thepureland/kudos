package io.kudos.tools.codegen.core

import io.kudos.base.io.FileKit
import io.kudos.tools.codegen.core.merge.CodeMerger
import io.kudos.tools.codegen.core.merge.PrivateContentEraser
import io.kudos.tools.codegen.model.vo.GenFile
import io.kudos.tools.codegen.service.CodeGenFileService
import io.kudos.tools.codegen.service.CodeGenObjectService
import java.io.File

/**
 * 代码生成器，代码生成核心逻辑处理
 *
 * @author K
 * @since 1.0.0
 */
class CodeGenerator(
    private val templateModel: Map<String, Any?>,
    private val genFiles: List<GenFile>
) {

    fun generate(needPersist : Boolean = true) {
        genFiles.forEach { executeGenerate(it) }
        if (needPersist) {
            persistence()
        }
    }

    private fun persistence(): Boolean {
        var success = CodeGenObjectService.saveOrUpdate()
        if (success) {
            success = io.kudos.tools.codegen.service.CodeGenColumnService.saveColumns(
                CodeGeneratorContext.tableName, CodeGeneratorContext.columns)
            if (success) {
                val filenames = genFiles.filter { it.getGenerate() }.map { it.getFilename() }
                success = CodeGenFileService.save(filenames)
            }
        }
        return success
    }

    private fun executeGenerate(genFile: GenFile) {
        val template = TemplateReader().read(genFile.templateFileRelativePath)
        val absoluteOutputFilePath =
            File("${CodeGeneratorContext.config.getCodeLoaction()}/${genFile.finalFileRelativePath}")
        val exists = absoluteOutputFilePath.exists()
        var codeMerger: CodeMerger? = null
        if (exists) {
            codeMerger = CodeMerger(absoluteOutputFilePath)
        } else {
            FileKit.touch(absoluteOutputFilePath)
        }
        FreemarkerKit.processTemplate(template, templateModel, absoluteOutputFilePath, "UTF-8")
        if (codeMerger != null) {
            codeMerger.merge()
        } else {
            PrivateContentEraser.erase(absoluteOutputFilePath)
        }
    }

}