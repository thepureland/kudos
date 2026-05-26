package io.kudos.ms.user.core.contact.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import org.ktorm.schema.boolean
import org.ktorm.schema.datetime
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * User contact way table-to-entity mapping.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object UserContactWays : StringIdTable<UserContactWay>("user_contact_way") {

    /** User id. */
    var userId = varchar("user_id").bindTo { it.userId }

    /** Contact way dict code. */
    var contactWayDictCode = varchar("contact_way_dict_code").bindTo { it.contactWayDictCode }

    /** Contact way value. */
    var contactWayValue = varchar("contact_way_value").bindTo { it.contactWayValue }

    /** Contact way status dict code. */
    var contactWayStatusDictCode = varchar("contact_way_status_dict_code").bindTo { it.contactWayStatusDictCode }

    /** Priority. */
    var priority = int("priority").bindTo { it.priority }

    /** Remark. */
    var remark = varchar("remark").bindTo { it.remark }

    /** Active flag. */
    var active = boolean("active").bindTo { it.active }

    /** Built-in flag. */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Creator id. */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name. */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Create time. */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id. */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name. */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time. */
    var updateTime = datetime("update_time").bindTo { it.updateTime }




}
