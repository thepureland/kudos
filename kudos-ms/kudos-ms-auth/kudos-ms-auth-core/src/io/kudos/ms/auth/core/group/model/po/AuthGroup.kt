package io.kudos.ms.auth.core.group.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * User group database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface AuthGroup : IDbEntity<String, AuthGroup> {

    companion object : DbEntityFactory<AuthGroup>()

    /** Group code. */
    var code: String

    /** Group name. */
    var name: String

    /** Tenant id. */
    var tenantId: String

    /** Subsystem code. */
    var subsysCode: String

    /** Remark. */
    var remark: String?

    /** Whether active. */
    var active: Boolean

    /** Whether built-in. */
    var builtIn: Boolean

    /** Creator id. */
    var createUserId: String?

    /** Creator name. */
    var createUserName: String?

    /** Creation time. */
    var createTime: LocalDateTime?

    /** Updater id. */
    var updateUserId: String?

    /** Updater name. */
    var updateUserName: String?

    /** Update time. */
    var updateTime: LocalDateTime?




}
