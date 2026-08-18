package io.kudos.ms.user.core.contact.model.table

import io.kudos.ability.data.rdb.ktorm.support.ManagedTable
import io.kudos.ms.user.core.contact.model.po.UserContactWay
import org.ktorm.schema.int
import org.ktorm.schema.varchar


/**
 * User contact way table-to-entity mapping.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
object UserContactWays : ManagedTable<UserContactWay>("user_contact_way") {

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




}
