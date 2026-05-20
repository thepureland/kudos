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
    /** 模板填充模型，由 [TemplateModelCreator] 生成 */
    private val templateModel: Map<String, Any?>,
    /** 本次要生成的文件清单 */
    private val genFiles: List<GenFile>
) {

    /**
     * 按 [genFiles] 顺序生成全部文件；可选是否把"生成记录"持久化到 codegen 自己的元数据表。
     * 表无关文件场景（[BatchGenerationController.generate] 的非实体生成）传 false 跳过持久化，
     * 避免把全局生成痕迹写到当前表上。
     *
     * @param needPersist 是否持久化生成记录，默认 true
     * @author K
     * @since 1.0.0
     */
    fun generate(needPersist : Boolean = true) {
        genFiles.forEach { executeGenerate(it) }
        if (needPersist) {
            persistence()
        }
    }

    /**
     * 把"对哪个表生成了哪些列、哪些文件"写入 codegen 自身的 H2 元数据库，供下次打开 wizard 时恢复勾选状态。
     * 任一步失败立即返回 false，不继续写后续表（避免出现部分一致的脏记录）。
     *
     * @return 三步（对象、列、文件）都成功才返回 true
     * @author K
     * @since 1.0.0
     */
    private fun persistence(): Boolean {
        if (!CodeGenObjectService.saveOrUpdate()) return false
        if (!io.kudos.tools.codegen.service.CodeGenColumnService.saveColumns(
                CodeGeneratorContext.tableName, CodeGeneratorContext.columns)) return false
        val filenames = genFiles.filter { it.getGenerate() }.map { it.getFilename() }
        return CodeGenFileService.save(filenames)
    }

    /**
     * 真正生成一个文件。
     *
     * 文件已存在时：先构造 [CodeMerger] 抓取旧文件中的用户自填代码与 import，
     * 然后 Freemarker 渲染覆盖落盘，再调 [CodeMerger.merge] 把用户内容融回新文件——
     * 顺序至关重要：先抓后写，否则用户自填代码会在 Freemarker 写盘那一刻丢失。
     *
     * 文件不存在时：touch 创建空文件 → Freemarker 渲染 → [PrivateContentEraser]
     * 清掉"模板私有标记区块"（标记区块仅用于辅助合并、不应出现在首次生成结果里）。
     *
     * @param genFile 待生成的文件
     * @author K
     * @since 1.0.0
     */
    private fun executeGenerate(genFile: GenFile) {
        val template = TemplateReader().read(genFile.templateFileRelativePath)
        val absoluteOutputFilePath =
            File("${CodeGeneratorContext.config.getCodeLoaction()}/${genFile.finalFileRelativePath}")
        // 已存在的目标文件先抓取用户自填内容（用于稍后合并）；不存在则 touch 出空文件
        val codeMerger = if (absoluteOutputFilePath.exists()) {
            CodeMerger(absoluteOutputFilePath)
        } else {
            FileKit.touch(absoluteOutputFilePath)
            null
        }
        FreemarkerKit.processTemplate(template, templateModel, absoluteOutputFilePath, "UTF-8")
        codeMerger?.merge() ?: PrivateContentEraser.erase(absoluteOutputFilePath)
    }

}