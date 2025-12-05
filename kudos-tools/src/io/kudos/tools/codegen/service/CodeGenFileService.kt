package io.kudos.tools.codegen.service

import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.dao.CodeGenFileDao
import io.kudos.tools.codegen.model.po.CodeGenFile


/**
 * 生成的文件历史信息服务
 *
 * @author K
 * @since 1.0.0
 */
object CodeGenFileService {

    fun read(): List<String> {
        return CodeGenFileDao.searchCodeGenFileNames(CodeGeneratorContext.tableName)
    }

    fun save(files: Collection<String>): Boolean {
        val filesInDb = read()
        val codeGenFileList = mutableListOf<CodeGenFile>()
        files.filter { !filesInDb.contains(it) }.forEach { file ->
            codeGenFileList.add(
                CodeGenFile {
                    filename = file
                    objectName = CodeGeneratorContext.tableName
                }
            )
        }
        return CodeGenFileDao.batchInsert(codeGenFileList) == codeGenFileList.size
    }

}