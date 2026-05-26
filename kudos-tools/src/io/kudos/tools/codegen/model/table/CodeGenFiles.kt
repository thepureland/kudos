package io.kudos.tools.codegen.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.tools.codegen.model.po.CodeGenFile
import org.ktorm.schema.varchar

/**
 * Code generation - file info table - entity binding object.
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object CodeGenFiles : StringIdTable<CodeGenFile>("code_gen_file") {
//endregion your codes 1

    /** File name */
    var filename = varchar("filename").bindTo { it.filename }

    /** Object name */
    var objectName = varchar("object_name").bindTo { it.objectName }


    //region your codes 2

    //endregion your codes 2

}