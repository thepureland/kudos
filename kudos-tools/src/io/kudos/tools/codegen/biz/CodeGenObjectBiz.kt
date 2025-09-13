package io.kudos.tools.codegen.biz

import io.kudos.ability.data.rdb.jdbc.kit.RdbMetadataKit
import io.kudos.ability.data.rdb.jdbc.metadata.TableTypeEnum
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.dao.CodeGenObjectDao
import io.kudos.tools.codegen.model.po.CodeGenObject
import java.time.LocalDateTime
import kotlin.collections.filter

/**
 * 生成的表对象历史信息服务
 *
 * @author K
 * @since 1.0.0
 */
object CodeGenObjectBiz {

    fun readTables(): Map<String, String?> {
        // from meta data
        val tables = RdbMetadataKit.getTablesByType(TableTypeEnum.TABLE, TableTypeEnum.VIEW)
        val nameAndComments = mutableMapOf<String, String?>()
        tables.filter {
            it.name !in setOf("code_gen_file", "code_gen_object", "code_gen_column")
                    && !it.name!!.startsWith("flyway_")
        }.forEach { nameAndComments[it.name!!] = it.comment }

        // from code_gen_object
        val codeGenObjects = CodeGenObjectDao.allSearch()
        for (codeGenObject in codeGenObjects) {
            if (nameAndComments.contains(codeGenObject.name)) {
                nameAndComments[codeGenObject.name] = codeGenObject.comment
            }
        }
        return nameAndComments
    }

    fun saveOrUpdate(): Boolean {
        val table = CodeGeneratorContext.tableName
        val comment = CodeGeneratorContext.tableComment
        val author = CodeGeneratorContext.config.getAuthor()
        val codeGenObject = CodeGenObjectDao.searchByName(table)
        return if (codeGenObject == null) {
            CodeGenObjectDao.insert(CodeGenObject {
                name = table
                this.comment = comment
                createTime = LocalDateTime.now()
                createUser = author
                genCount = 1
            })
            true
        } else {
            with(codeGenObject) {
                this.comment = comment
                updateTime = LocalDateTime.now()
                updateUser = author
                genCount = codeGenObject.genCount + 1
            }
            CodeGenObjectDao.update(codeGenObject)
        }
    }

}