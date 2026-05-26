package io.kudos.tools.codegen.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * Code generation - object info database entity.
 *
 * @author K
 * @since 1.0.0
 */
//region your codes 1
interface CodeGenObject : IDbEntity<String, CodeGenObject> {
//endregion your codes 1

    companion object : DbEntityFactory<CodeGenObject>()

    /** Object name */
    var name: String

    /** Comment */
    var comment: String?

    /** Create time */
    var createTime: LocalDateTime

    /** Create user */
    var createUser: String

    /** Update time */
    var updateTime: LocalDateTime?

    /** Update user */
    var updateUser: String?

    /** Generation count */
    var genCount: Int


    //region your codes 2

    //endregion your codes 2

}