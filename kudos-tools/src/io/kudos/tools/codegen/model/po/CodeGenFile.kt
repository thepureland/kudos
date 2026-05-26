package io.kudos.tools.codegen.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity


/**
 * Code generation - file info database entity.
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface CodeGenFile : IDbEntity<String, CodeGenFile> {
//endregion your codes 1

    companion object : DbEntityFactory<CodeGenFile>()

    /** File name */
    var filename: String

    /** Object name */
    var objectName: String


    //region your codes 2

    //endregion your codes 2

}