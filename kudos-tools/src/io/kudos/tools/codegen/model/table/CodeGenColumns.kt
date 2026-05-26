package io.kudos.tools.codegen.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.tools.codegen.model.po.CodeGenColumn
import org.ktorm.schema.boolean
import org.ktorm.schema.varchar

/**
 * Code generation - column info table - entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object CodeGenColumns : StringIdTable<CodeGenColumn>("code_gen_column") {
//endregion your codes 1

    /** Field name */
    var name = varchar("name").bindTo { it.name }

    /** Object name */
    var objectName = varchar("object_name").bindTo { it.objectName }

    /** Comment */
    var comment = varchar("comment").bindTo { it.comment }

    /** Whether it is a search item */
    var searchItem = boolean("search_item").bindTo { it.searchItem }

    /** Whether it is a list item */
    var listItem = boolean("list_item").bindTo { it.listItem }

    /** Whether it is an edit item */
    var editItem = boolean("edit_item").bindTo { it.editItem }

    /** Whether it is a detail item */
    var detailItem = boolean("detail_item").bindTo { it.detailItem }

    /** Whether it is a cache item */
    var cacheItem = boolean("cache_item").bindTo { it.cacheItem }

    //region your codes 2

    //endregion your codes 2

}