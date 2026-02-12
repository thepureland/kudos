package io.kudos.tools.codegen.core

import freemarker.template.Configuration
import io.kudos.base.io.FileKit
import io.kudos.base.io.FilenameKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.tools.codegen.model.vo.GenFile
import org.apache.commons.io.filefilter.IOFileFilter
import java.io.File

object TemplatePathProcessor {

    fun readPaths(includeEntityRelativeFile: Boolean): List<GenFile> {
        val templateRootDir = CodeGeneratorContext.config.getTemplateInfo().rootDir
        val fileFilter: IOFileFilter = object : IOFileFilter {
            override fun accept(file: File): Boolean {
                return "macro.include" != file.name
            }

            override fun accept(file: File, s: String): Boolean {
                return "macro.include" != s
            }
        }
        val templateFiles = if (templateRootDir.contains(".jar")) {
            jarFiles
        } else {
            FileKit.listFiles(File(templateRootDir), fileFilter, fileFilter)
        }
        val templateModel = if (includeEntityRelativeFile) {
            CodeGeneratorContext.templateModelCreator.create()
        } else {
            CodeGeneratorContext.templateModelCreator.createBaseModel()
        }
        val cfg = Configuration(Configuration.VERSION_2_3_30)
        val genFiles = mutableListOf<GenFile>()
        for (file in templateFiles) {
            val absolutePath = FilenameKit.normalize(file.absolutePath, true)
            val templateFileRelativePath = absolutePath.substring(templateRootDir.lastIndex + 2)
            // 不是实体相关的模板
            val notEntityRelative = !isEntityRelative(templateFileRelativePath)
                    && !isEntityRelative(TemplateReader().read(templateFileRelativePath).toString())
            if (!notEntityRelative && !includeEntityRelativeFile) { // 当前为实体相关的模板，但是要求返回的是不包括实体相关的模板时
                continue
            } else {
                val filename = FreemarkerKit.processTemplateString(file.name, templateModel, cfg)
                var directory = FreemarkerKit.processTemplateString(file.parent, templateModel, cfg)
                directory = FilenameKit.normalize(directory, true)
                val destRelativeDirectory = directory.substring(templateRootDir.length + 1).replace('.', '/')
                val finalFileRelativePath = "$destRelativeDirectory/$filename"
                genFiles.add(
                    GenFile(
                        false, filename,
                        "${CodeGeneratorContext.config.getCodeLoaction()}/${destRelativeDirectory}",
                        finalFileRelativePath, templateFileRelativePath
                    )
                )
            }
        }
        genFiles.sort()
        return genFiles
    }

    /**
     * 获取jar内文件
     */
    private val jarFiles: Collection<File>
        get() {
            val resources =
                ClassPathScanner.scanForResources(CodeGeneratorContext.config.getTemplateInfo().rootDir, "", "")
            val files = mutableListOf<File>()
            for (resource in resources) {
                if (resource.filename.isNotBlank() && !resource.filename.contains("macro.include")) {
                    resource.locationOnDisk?.let { files.add(File(it)) }
                }
            }
            return files
        }

    private fun isEntityRelative(content: String): Boolean = content.contains($$"${entityName}")

}