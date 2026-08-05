package io.kudos.ms.auth.core.group.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Group-user relation database entity.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthGroupUser : IDbEntity<String, AuthGroupUser> {

    companion object : DbEntityFactory<AuthGroupUser>()

    /** Group id. */
    var groupId: String

    /** User id. */
    var userId: String

    /** Subject kind: USER / SERVICE / API_KEY. */
    var principalType: String?

    /** Membership effective time; NULL = effective immediately. */
    var startTime: LocalDateTime?

    /** Membership expiry time; NULL = never expires. */
    var endTime: LocalDateTime?

    /** Who put this principal into the group (permission semantics, not an audit stamp). */
    var grantedBy: String?

    /** The upstream grant this membership was delegated from; NULL = direct. */
    var parentGrantId: String?

    /** Soft revocation: the row survives so the chain and audit trail do too. */
    var revoked: Boolean?

    /** Why the membership was revoked. */
    var revokeReason: String?

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
