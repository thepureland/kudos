package io.kudos.tools.codegen.core

import io.kudos.base.io.FileKit
import io.kudos.base.logger.LogFactory
import io.kudos.tools.codegen.core.merge.CodeMerger
import io.kudos.tools.codegen.core.merge.PrivateContentEraser
import io.kudos.tools.codegen.model.vo.GenFile
import io.kudos.tools.codegen.service.CodeGenColumnService
import io.kudos.tools.codegen.service.CodeGenFileService
import io.kudos.tools.codegen.service.CodeGenObjectService
import java.io.File

/**
 * Code generator: core logic for code generation.
 *
 * @author K
 * @since 1.0.0
 */
class CodeGenerator(
    /** Template fill model produced by [TemplateModelCreator] */
    private val templateModel: Map<String, Any?>,
    /** List of files to generate in this run */
    private val genFiles: List<GenFile>
) {

    /**
     * Generates every file in [genFiles] order; optionally persists the "generation record" into
     * codegen's own metadata tables. Table-agnostic scenarios (the non-entity generation in
     * [BatchGenerationController.generate]) pass false to skip persistence so a global generation
     * footprint is not attributed to the current table.
     *
     * @param needPersist whether to persist the generation record, default true
     * @author K
     * @since 1.0.0
     */
    fun generate(needPersist: Boolean = true) {
        genFiles.forEach { executeGenerate(it) }
        if (needPersist && !persistence()) {
            log.warn("Failed to persist the generation record for table: ${CodeGeneratorContext.tableName}")
        }
    }

    /**
     * Writes "for which table, which columns and which files were generated" into codegen's own
     * H2 metadata database so the wizard can restore selection state on the next open.
     * Returns false on the first failed step rather than continuing — to avoid partially consistent
     * dirty records.
     *
     * @return true only if all three steps (object, columns, files) succeed
     * @author K
     * @since 1.0.0
     */
    private fun persistence(): Boolean {
        if (!CodeGenObjectService.saveOrUpdate()) return false
        if (!CodeGenColumnService.saveColumns(CodeGeneratorContext.tableName, CodeGeneratorContext.columns)) {
            return false
        }
        val filenames = genFiles.filter { it.getGenerate() }.map { it.getFilename() }
        return CodeGenFileService.save(filenames)
    }

    /**
     * Actually generates a single file.
     *
     * If the file already exists: build a [CodeMerger] first to capture the user's hand-written code
     * and imports from the old file, then let Freemarker render and overwrite the file, then call
     * [CodeMerger.merge] to merge the user content back in. The ordering is critical: capture before
     * writing, otherwise user-written code is lost the moment Freemarker writes to disk.
     *
     * If the file does not exist: touch an empty file → Freemarker render → [PrivateContentEraser]
     * strips the "template-private marker blocks" (the marker blocks are only used to assist
     * merging and should not appear in a first-time generation result).
     *
     * @param genFile the file to generate
     * @author K
     * @since 1.0.0
     */
    private fun executeGenerate(genFile: GenFile) {
        val template = TemplateReader().read(genFile.templateFileRelativePath)
        val absoluteOutputFilePath =
            File("${CodeGeneratorContext.config.getCodeLoaction()}/${genFile.finalFileRelativePath}")
        // Existing target file: capture user-written content for later merge; otherwise touch a new empty file
        val codeMerger = if (absoluteOutputFilePath.exists()) {
            CodeMerger(absoluteOutputFilePath)
        } else {
            FileKit.touch(absoluteOutputFilePath)
            null
        }
        FreemarkerKit.processTemplate(template, templateModel, absoluteOutputFilePath, "UTF-8")
        codeMerger?.merge() ?: PrivateContentEraser.erase(absoluteOutputFilePath)
    }

    private val log = LogFactory.getLog(this::class)

}