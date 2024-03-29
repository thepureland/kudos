package io.kudos.tools.codegen.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.tools.codegen.model.po.CodeGenColumn
import org.ktorm.schema.boolean
import org.ktorm.schema.varchar

/**
 * 代码生成-列信息数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object CodeGenColumns : StringIdTable<CodeGenColumn>("code_gen_column") {
//endregion your codes 1

    /** 字段名 */
    var name = varchar("name").bindTo { it.name }

    /** 对象名称 */
    var objectName = varchar("object_name").bindTo { it.objectName }

    /** 注释 */
    var comment = varchar("comment").bindTo { it.comment }

    /** 是否查询项 */
    var searchItem = boolean("search_item").bindTo { it.searchItem }

    /** 是否列表项 */
    var listItem = boolean("list_item").bindTo { it.listItem }

    /** 是否编辑项 */
    var editItem = boolean("edit_item").bindTo { it.editItem }

    /** 是否详情项 */
    var detailItem = boolean("detail_item").bindTo { it.detailItem }

    /** 是否缓存项 */
    var cacheItem = boolean("cache_item").bindTo { it.cacheItem }

    //region your codes 2

    //endregion your codes 2

}