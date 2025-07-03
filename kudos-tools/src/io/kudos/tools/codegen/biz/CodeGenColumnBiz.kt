package io.kudos.tools.codegen.biz

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
object CodeGenColumnBiz {

    fun readColumns(tableName : String): List<ColumnInfo> {
        // from meta data
        val columns = RdbMetadataKit.getColumnsByTableName(tableName)

        // from code_gen_column table
        val columnMap = CodeGenColumnDao.searchCodeGenColumnMap(tableName)

        // merge
        val results = mutableListOf<ColumnInfo>()
        for (column in columns.values) {
            val codeGenColumn = columnMap[column.name]
            val columnInfo = ColumnInfo()
            results.add(columnInfo)
            if (codeGenColumn != null) { // old column
                BeanKit.copyProperties(codeGenColumn, columnInfo)
                columnInfo.setCustomComment(codeGenColumn.comment)
                columnInfo.setOrigComment(column.comment)
            } else {
                with(columnInfo) {
                    setName(column.name)
                    setOrigComment(column.comment)
                }
            }
        }
        if (columnMap.isEmpty()) { // 默认都为详情项
            results.map { it.setDetailItem(true) }
        }
        return results
    }

    fun saveColumns(tableName : String, columnInfos: List<ColumnInfo>): Boolean {
        // delete old columns first
        CodeGenColumnDao.deleteCodeGenColumn(tableName)

        // insert new columns
        val codeGenColumns = mutableListOf<CodeGenColumn>()
        for (column in columnInfos) {
            codeGenColumns.add(
                CodeGenColumn {
                    name = column.getName()!!
                    objectName = tableName
                    comment = column.getCustomComment()
                    searchItem = column.getSearchItem()
                    listItem = column.getListItem()
                    editItem = column.getEditItem()
                    detailItem = column.getDetailItem()
                    cacheItem = column.getCacheItem()
                }
            )
        }
        return CodeGenColumnDao.batchInsert(codeGenColumns) == codeGenColumns.size
    }

}