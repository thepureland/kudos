package io.kudos.tools.codegen.core

import freemarker.template.Configuration
import io.kudos.base.io.FileKit
import io.kudos.base.io.FilenameKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.tools.codegen.model.vo.GenFile
import org.apache.commons.io.filefilter.IOFileFilter
import java.io.File

/**
 * 模板路径处理器。
 *
 * 负责：
 * 1. 扫描模板根目录（文件系统 / JAR 两种来源）下的所有模板文件，剔除 `macro.include` 这类辅助文件；
 * 2. 用 Freemarker 处理路径中的占位符（`${entityName}` 等），把模板路径映射成目标输出路径；
 * 3. 根据"是否需要实体相关模板"过滤，让单表/多表场景共用同一份扫描逻辑。
 *
 * @author K
 * @since 1.0.0
 */
object TemplatePathProcessor {

    /**
     * 扫描模板根目录并把每个模板映射成 [GenFile]。
     * 模板根目录里若包含 `.jar` 路径片段，认为模板打在 JAR 包里，转走 [jarFiles] 用 classpath 扫描；
     * 否则视为本地目录，直接 [FileKit.listFiles] 递归。
     *
     * @param includeEntityRelativeFile true 时连同实体相关模板一起返回；false 时跳过任何含 `${entityName}` 的模板
     * @return 排序后的待生成文件列表
     * @author K
     * @since 1.0.0
     */
    fun readPaths(includeEntityRelativeFile: Boolean): List<GenFile> {
        val templateRootDir = CodeGeneratorContext.config.getTemplateInfo().rootDir
        val fileFilter: IOFileFilter = object : IOFileFilter {
            override fun accept(file: File): Boolean = "macro.include" != file.name
            override fun accept(file: File, s: String): Boolean = "macro.include" != s
        }
        val templateFiles = if (templateRootDir.contains(".jar")) jarFiles
            else FileKit.listFiles(File(templateRootDir), fileFilter, fileFilter)
        val templateModel = if (includeEntityRelativeFile) CodeGeneratorContext.templateModelCreator.create()
            else CodeGeneratorContext.templateModelCreator.createBaseModel()
        val cfg = Configuration(Configuration.VERSION_2_3_30)
        return templateFiles.mapNotNull { file ->
            val absolutePath = FilenameKit.normalize(file.absolutePath, true)
            val templateFileRelativePath = absolutePath.substring(templateRootDir.lastIndex + 2)
            // 不是实体相关的模板
            val notEntityRelative = !isEntityRelative(templateFileRelativePath)
                && !isEntityRelative(TemplateReader().read(templateFileRelativePath).toString())
            // 当前为实体相关的模板，但是要求返回的是不包括实体相关的模板时
            if (!notEntityRelative && !includeEntityRelativeFile) return@mapNotNull null
            val filename = FreemarkerKit.processTemplateString(file.name, templateModel, cfg)
            val directory = FilenameKit.normalize(
                FreemarkerKit.processTemplateString(file.parent, templateModel, cfg), true
            )
            val destRelativeDirectory = directory.substring(templateRootDir.length + 1).replace('.', '/')
            GenFile(
                false, filename,
                "${CodeGeneratorContext.config.getCodeLoaction()}/$destRelativeDirectory",
                "$destRelativeDirectory/$filename", templateFileRelativePath
            )
        }.sorted()
    }

    /**
     * 获取jar内文件
     */
    private val jarFiles: Collection<File>
        get() = ClassPathScanner
            .scanForResources(CodeGeneratorContext.config.getTemplateInfo().rootDir, "", "")
            .filter { it.filename.isNotBlank() && !it.filename.contains("macro.include") }
            .mapNotNull { it.locationOnDisk?.let(::File) }

    /**
     * 判断内容（路径或模板正文）是否引用了 `${entityName}` 占位符。
     * 用作"实体相关 vs 通用"模板的判别条件。
     *
     * @param content 待检测字符串
     * @return true 表示是实体相关模板
     * @author K
     * @since 1.0.0
     */
    private fun isEntityRelative(content: String): Boolean = content.contains($$"${entityName}")

}