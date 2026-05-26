package io.kudos.ability.data.rdb.ktorm.support

import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.varchar

/**
 * Database entity with management fields (primary key type is string).
 *
 * @param E entity type
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
open class ManagedTable<E : IManagedDbEntity<String, E>>(tableName: String): StringIdTable<E>(tableName) {

    /** Record creation time */
    val createTime = datetime("create_time").bindTo { it.createTime }

    /** ID of the record creator */
    val createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Name of the record creator */
    val createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Record update time */
    val updateTime = datetime("update_time").bindTo { it.updateTime }

    /** ID of the last updater */
    val updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Name of the last updater */
    val updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Whether enabled */
    val active = boolean("active").bindTo { it.active }

    /** Whether built-in */
    val builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Remark */
    val remark = varchar("remark").bindTo { it.remark }

}
