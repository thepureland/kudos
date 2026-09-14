package io.kudos.ms.tag.core.persistence

import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity
import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/** Management-column mapping for tag tables whose domain has an explicit description instead of a generic remark. */
abstract class TagManagedTable<E : IManagedDbEntity<String, E>>(tableName: String) : StringIdTable<E>(tableName) {
    val createTime = datetime("create_time").bindTo { it.createTime }
    val createUserId = varchar("create_user_id").bindTo { it.createUserId }
    val createUserName = varchar("create_user_name").bindTo { it.createUserName }
    val updateTime = datetime("update_time").bindTo { it.updateTime }
    val updateUserId = varchar("update_user_id").bindTo { it.updateUserId }
    val updateUserName = varchar("update_user_name").bindTo { it.updateUserName }
    val active = boolean("active").bindTo { it.active }
    val builtIn = boolean("built_in").bindTo { it.builtIn }
}
