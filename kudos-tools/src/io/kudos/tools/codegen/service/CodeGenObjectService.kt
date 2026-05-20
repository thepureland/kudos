package io.kudos.tools.codegen.service

import io.kudos.ability.data.rdb.jdbc.kit.RdbMetadataKit
import io.kudos.ability.data.rdb.jdbc.metadata.TableTypeEnum
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.dao.CodeGenObjectDao
import io.kudos.tools.codegen.model.po.CodeGenObject
import java.time.LocalDateTime

/**
 * 生成的表对象历史信息服务
 *
 * @author K
 * @since 1.0.0
 */
object CodeGenObjectService {

    fun readTables(): Map<String, String?> {
        // from meta data
        val nameAndComments = RdbMetadataKit.getTablesByType(TableTypeEnum.TABLE, TableTypeEnum.VIEW)
            .asSequence()
            .mapNotNull { t -> t.name?.let { it to t.comment } }
            .filter { (n, _) -> n !in EXCLUDED_TABLES && !n.startsWith("flyway_") }
            .toMap(mutableMapOf())

        // from code_gen_object：覆盖元数据中已有条目的注释（用户自填注释优先）
        CodeGenObjectDao.allSearch()
            .filter { it.name in nameAndComments }
            .forEach { nameAndComments[it.name] = it.comment }
        return nameAndComments
    }

    private val EXCLUDED_TABLES = setOf("code_gen_file", "code_gen_object", "code_gen_column")

    fun saveOrUpdate(): Boolean {
        val tableComment = CodeGeneratorContext.tableComment
        val author = CodeGeneratorContext.config.getAuthor()
        val existing = CodeGenObjectDao.searchByName(CodeGeneratorContext.tableName)
        if (existing == null) {
            CodeGenObjectDao.insert(CodeGenObject {
                name = CodeGeneratorContext.tableName
                comment = tableComment
                createTime = LocalDateTime.now()
                createUser = author
                genCount = 1
            })
            return true
        }
        return CodeGenObjectDao.update(existing.apply {
            comment = tableComment
            updateTime = LocalDateTime.now()
            updateUser = author
            genCount += 1
        })
    }

}