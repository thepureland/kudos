package io.kudos.tools.codegen.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.tools.codegen.model.po.CodeGenFile
import org.ktorm.schema.varchar

/**
 * 代码生成-文件信息数据库表-实体关联对象
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
object CodeGenFiles : StringIdTable<CodeGenFile>("code_gen_file") {
//endregion your codes 1

    /** 文件名 */
    var filename = varchar("filename").bindTo { it.filename }

    /** 对象名 */
    var objectName = varchar("object_name").bindTo { it.objectName }


    //region your codes 2

    //endregion your codes 2

}