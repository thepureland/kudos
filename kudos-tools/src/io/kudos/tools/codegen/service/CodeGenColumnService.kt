package io.kudos.tools.codegen.service

import io.kudos.ability.data.rdb.jdbc.kit.RdbMetadataKit
import io.kudos.base.bean.BeanKit
import io.kudos.tools.codegen.dao.CodeGenColumnDao
import io.kudos.tools.codegen.model.po.CodeGenColumn
import io.kudos.tools.codegen.model.vo.ColumnInfo


/**
 * 生成的列信息服务
 *
 * @author K
 * @since 1.0.0
 */
object CodeGenColumnService {

    fun readColumns(tableName: String): List<ColumnInfo> {
        // from meta data
        val columns = RdbMetadataKit.getColumnsByTableName(tableName)
        // from code_gen_column table
        val columnMap = CodeGenColumnDao.searchCodeGenColumnMap(tableName)

        // merge
        val results = columns.values.map { column ->
            val columnInfo = ColumnInfo()
            val codeGenColumn = columnMap[column.name]
            if (codeGenColumn != null) {
                BeanKit.copyProperties(codeGenColumn, columnInfo)
                columnInfo.setCustomComment(codeGenColumn.comment)
                columnInfo.setOrigComment(column.comment)
            } else {
                columnInfo.setName(column.name)
                columnInfo.setOrigComment(column.comment)
            }
            columnInfo
        }
        if (columnMap.isEmpty()) results.forEach(::applyDefaults)
        return results
    }

    private fun applyDefaults(it: ColumnInfo) {
        // 默认都为详情项 / 缓存项
        it.setDetailItem(true)
        it.setCacheItem(true)
        val columnName = requireNotNull(it.getName()) { "column name is null" }.lowercase()
        if (columnName !in SKIP_SEARCH) it.setSearchItem(true)
        if (columnName !in SKIP_LIST) it.setListItem(true)
        if (columnName !in SKIP_EDIT) it.setEditItem(true)
    }

    private val SKIP_SEARCH = setOf("id", "built_in", "create_user", "create_time", "update_user", "update_time")
    private val SKIP_LIST = setOf("id", "create_user", "create_time", "update_user", "update_time")
    private val SKIP_EDIT = setOf("id", "built_in", "create_user", "create_time", "update_user", "update_time")

    fun saveColumns(tableName: String, columnInfos: List<ColumnInfo>): Boolean {
        // delete old columns first
        CodeGenColumnDao.deleteCodeGenColumn(tableName)
        // insert new columns
        val codeGenColumns = columnInfos.map { column ->
            CodeGenColumn {
                name = requireNotNull(column.getName()) { "column name is null" }
                objectName = tableName
                comment = column.getCustomComment()
                searchItem = column.getSearchItem()
                listItem = column.getListItem()
                editItem = column.getEditItem()
                detailItem = column.getDetailItem()
                cacheItem = column.getCacheItem()
            }
        }
        return CodeGenColumnDao.batchInsert(codeGenColumns) == codeGenColumns.size
    }

}