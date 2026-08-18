package io.kudos.ms.user.core.contact.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IManagedDbEntity

/**
 * User contact way database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface UserContactWay : IManagedDbEntity<String, UserContactWay> {

    companion object : DbEntityFactory<UserContactWay>()

    /** User id. */
    var userId: String

    /** Contact way dict code. */
    var contactWayDictCode: String

    /** Contact way value. */
    var contactWayValue: String

    /** Contact way status dict code. */
    var contactWayStatusDictCode: String

    /** Priority. */
    var priority: Int?




}
