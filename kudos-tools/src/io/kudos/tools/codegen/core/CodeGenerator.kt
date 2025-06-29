package io.kudos.tools.codegen.core

import freemarker.cache.MultiTemplateLoader
import freemarker.cache.URLTemplateLoader
import freemarker.template.Configuration
import io.kudos.base.io.FileKit
import io.kudos.base.logger.LogFactory
import io.kudos.tools.codegen.biz.CodeGenFileBiz
import io.kudos.tools.codegen.biz.CodeGenObjectBiz
import io.kudos.tools.codegen.core.merge.CodeMerger
import io.kudos.tools.codegen.core.merge.PrivateContentEraser
import io.kudos.tools.codegen.model.vo.GenFile
import javafx.scene.control.Alert
import java.io.File
import java.net.URI
import java.net.URL

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

    fun generate() {
        genFiles.forEach { executeGenerate(it) }
        val persistence = persistence()
        if (persistence) {
            Alert(
                Alert.AlertType.INFORMATION, "生成成功，请查看目录：${CodeGeneratorContext.config.getCodeLoaction()}".trimIndent()
            ).show()
        } else {
            Alert(Alert.AlertType.INFORMATION, "生成成功，但配置信息持久化失败! ").show()
        }
    }

    private fun persistence(): Boolean {
        var success = CodeGenObjectBiz.saveOrUpdate()
        if (success) {
            success = io.kudos.tools.codegen.biz.CodeGenColumnBiz.saveColumns()
            if (success) {
                val filenames = genFiles.filter { it.getGenerate() }.map { it.getFilename() }
                success = CodeGenFileBiz.save(filenames)
            }
        }
        return success
    }

    private fun newFreeMarkerConfiguration(): Configuration {
        val templateRootDir = CodeGeneratorContext.config.getTemplateInfo()!!.rootDir
        val root = URI("file:$templateRootDir").toURL()
        val multiTemplateLoader = MultiTemplateLoader(arrayOf(
            object : URLTemplateLoader() {
                override fun getURL(template: String): URL = root.toURI().resolve(template).toURL()
            }
        ))
        val conf = Configuration(Configuration.VERSION_2_3_30)
        conf.templateLoader = multiTemplateLoader
        conf.numberFormat = "###############"
        conf.booleanFormat = "true,false"
        conf.defaultEncoding = "UTF-8"
        conf.setClassForTemplateLoading(
            CodeGenerator::class.java,
            "/templates/${CodeGeneratorContext.config.getTemplateInfo()!!.name}"
        )
        val autoIncludes = listOf("macro.include")
        val availableAutoInclude = FreemarkerKit.getAvailableAutoInclude(conf, autoIncludes)
        conf.setAutoIncludes(availableAutoInclude)
        log.debug("set Freemarker.autoIncludes:$availableAutoInclude for templateName:$templateRootDir autoIncludes:$autoIncludes")
        return conf
    }

    private fun executeGenerate(genFile: GenFile) {
        val template = newFreeMarkerConfiguration().getTemplate(genFile.templateFileRelativePath)
        template.outputEncoding = "UTF-8"
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