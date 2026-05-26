package io.kudos.ms.user.core.contact.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * User contact way database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface UserContactWay : IDbEntity<String, UserContactWay> {

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

    /** Remark. */
    var remark: String?

    /** Active flag. */
    var active: Boolean

    /** Built-in flag. */
    var builtIn: Boolean

    /** Creator id. */
    var createUserId: String?

    /** Creator name. */
    var createUserName: String?

    /** Create time. */
    var createTime: LocalDateTime?

    /** Updater id. */
    var updateUserId: String?

    /** Updater name. */
    var updateUserName: String?

    /** Update time. */
    var updateTime: LocalDateTime?




}
