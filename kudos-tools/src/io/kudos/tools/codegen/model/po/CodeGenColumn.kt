package io.kudos.tools.codegen.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity


/**
 * Code generation - column info database entity.
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface CodeGenColumn : IDbEntity<String, CodeGenColumn> {
//endregion your codes 1

    companion object : DbEntityFactory<CodeGenColumn>()

    /** Field name */
    var name: String

    /** Object name */
    var objectName: String

    /** Comment */
    var comment: String?

    /** Whether it is a search item */
    var searchItem: Boolean

    /** Whether it is a list item */
    var listItem: Boolean

    /** Whether it is an edit item */
    var editItem: Boolean

    /** Whether it is a detail item */
    var detailItem: Boolean

    /** Whether it is a cache item */
    var cacheItem: Boolean


    //region your codes 2

    //endregion your codes 2

}