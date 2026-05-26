package io.kudos.tools.codegen.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.tools.codegen.model.po.CodeGenObject
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar

/**
 * Code generation - object info table - entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object CodeGenObjects : StringIdTable<CodeGenObject>("code_gen_object") {
//endregion your codes 1

    /** Object name */
    var name = varchar("name").bindTo { it.name }

    /** Comment */
    var comment = varchar("comment").bindTo { it.comment }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Create user */
    var createUser = varchar("create_user").bindTo { it.createUser }

    /** Update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }

    /** Update user */
    var updateUser = varchar("update_user").bindTo { it.updateUser }

    /** Generation count */
    var genCount = int("gen_count").bindTo { it.genCount }


    //region your codes 2

    //endregion your codes 2

}