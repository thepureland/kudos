package io.kudos.tools.codegen.service

import io.kudos.ability.data.rdb.jdbc.kit.RdbMetadataKit
import io.kudos.ability.data.rdb.jdbc.metadata.TableTypeEnum
import io.kudos.tools.codegen.core.CodeGeneratorContext
import io.kudos.tools.codegen.dao.CodeGenObjectDao
import io.kudos.tools.codegen.model.po.CodeGenObject
import java.time.LocalDateTime

/**
 * Service for the history of generated table objects.
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

        // from code_gen_object: override comments from metadata (user-supplied comments take precedence)
        CodeGenObjectDao.allSearch()
            .filter { it.name in nameAndComments }
            .forEach { nameAndComments[it.name] = it.comment }
        return nameAndComments
    }

    private val EXCLUDED_TABLES = setOf("code_gen_file", "code_gen_object", "code_gen_column")

    /**
     * Previously entered business modules, keyed by table name. Blank entries are dropped so callers can
     * simply fall back to [io.kudos.tools.codegen.core.TemplateModelCreator.defaultBizModule].
     *
     * @return table name -> business module, only for tables that have a non-blank one recorded
     * @author K
     * @since 1.0.0
     */
    fun readBizModules(): Map<String, String> =
        CodeGenObjectDao.allSearch()
            .mapNotNull { obj -> obj.bizModule?.takeIf { it.isNotBlank() }?.let { obj.name to it } }
            .toMap()

    fun saveOrUpdate(): Boolean {
        val tableComment = CodeGeneratorContext.tableComment
        val bizModuleValue = CodeGeneratorContext.bizModule.takeIf { it.isNotBlank() }
        val author = CodeGeneratorContext.config.getAuthor()
        val existing = CodeGenObjectDao.searchByName(CodeGeneratorContext.tableName)
        if (existing == null) {
            CodeGenObjectDao.insert(CodeGenObject {
                name = CodeGeneratorContext.tableName
                comment = tableComment
                bizModule = bizModuleValue
                createTime = LocalDateTime.now()
                createUser = author
                genCount = 1
            })
            return true
        }
        return CodeGenObjectDao.update(existing.apply {
            comment = tableComment
            // Keep the previously recorded value when the current run left the field empty.
            bizModule = bizModuleValue ?: bizModule
            updateTime = LocalDateTime.now()
            updateUser = author
            genCount += 1
        })
    }

}