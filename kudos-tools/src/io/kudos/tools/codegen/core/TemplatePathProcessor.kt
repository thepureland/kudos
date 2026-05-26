package io.kudos.tools.codegen.core

import freemarker.template.Configuration
import io.kudos.base.io.FileKit
import io.kudos.base.io.FilenameKit
import io.kudos.base.io.scanner.classpath.ClassPathScanner
import io.kudos.tools.codegen.model.vo.GenFile
import org.apache.commons.io.filefilter.IOFileFilter
import java.io.File

/**
 * Template path processor.
 *
 * Responsibilities:
 * 1. Scans every template file under the template root (filesystem or JAR), filtering out helper
 *    files like `macro.include`.
 * 2. Uses Freemarker to resolve placeholders (e.g. `${entityName}`) in paths, mapping template
 *    paths into target output paths.
 * 3. Filters by "whether entity-related templates are needed" so the single-table and multi-table
 *    scenarios share the same scanning logic.
 *
 * @author K
 * @since 1.0.0
 */
object TemplatePathProcessor {

    /**
     * Scans the template root directory and maps every template into a [GenFile].
     * If the template root contains a `.jar` path segment, the templates are assumed to live inside
     * a JAR and are scanned through [jarFiles] via classpath; otherwise the root is treated as a
     * local directory and recursed via [FileKit.listFiles].
     *
     * @param includeEntityRelativeFile true to return entity-related templates as well; false to skip
     *        any template containing `${entityName}`
     * @return sorted list of files to be generated
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
            // Template is not entity-related
            val notEntityRelative = !isEntityRelative(templateFileRelativePath)
                && !isEntityRelative(TemplateReader().read(templateFileRelativePath).toString())
            // Current template is entity-related, but caller asked for non-entity-related only
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
     * Lists files inside the JAR-packaged template root.
     */
    private val jarFiles: Collection<File>
        get() = ClassPathScanner
            .scanForResources(CodeGeneratorContext.config.getTemplateInfo().rootDir, "", "")
            .filter { it.filename.isNotBlank() && !it.filename.contains("macro.include") }
            .mapNotNull { it.locationOnDisk?.let(::File) }

    /**
     * Tests whether the given content (path or template body) references the `${entityName}` placeholder.
     * Used as the discriminator between "entity-related" and "generic" templates.
     *
     * @param content string to test
     * @return true if it is an entity-related template
     * @author K
     * @since 1.0.0
     */
    private fun isEntityRelative(content: String): Boolean = content.contains($$"${entityName}")

}