package io.kudos.tools.codegen.core

import io.kudos.base.io.FileKit
import io.kudos.base.logger.LogFactory
import io.kudos.tools.codegen.biz.CodeGenFileBiz
import io.kudos.tools.codegen.biz.CodeGenObjectBiz
import io.kudos.tools.codegen.core.merge.CodeMerger
import io.kudos.tools.codegen.core.merge.PrivateContentEraser
import io.kudos.tools.codegen.model.vo.GenFile
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

    private val log = LogFactory.getLog(this)

    fun generate(needPersist : Boolean = true) {
        genFiles.forEach { executeGenerate(it) }
        if (needPersist) {
            persistence()
        }
    }

    private fun persistence(): Boolean {
        var success = CodeGenObjectBiz.saveOrUpdate()
        if (success) {
            success = io.kudos.tools.codegen.biz.CodeGenColumnBiz.saveColumns(
                CodeGeneratorContext.tableName, CodeGeneratorContext.columns)
            if (success) {
                val filenames = genFiles.filter { it.getGenerate() }.map { it.getFilename() }
                success = CodeGenFileBiz.save(filenames)
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