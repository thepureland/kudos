package io.kudos.tools.codegen.service

import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.dao.CodeGenFileDao
import io.kudos.tools.codegen.model.po.CodeGenFile


/**
 * Service for the history of generated files.
 *
 * @author K
 * @since 1.0.0
 */
object CodeGenFileService {

    fun read(): List<String> = CodeGenFileDao.searchCodeGenFileNames(CodeGeneratorContext.tableName)

    fun save(files: Collection<String>): Boolean {
        val filesInDb = read().toSet()
        val codeGenFileList = files.filterNot { it in filesInDb }.map { file ->
            CodeGenFile {
                filename = file
                objectName = CodeGeneratorContext.tableName
            }
        }
        return CodeGenFileDao.batchInsert(codeGenFileList) == codeGenFileList.size
    }

}