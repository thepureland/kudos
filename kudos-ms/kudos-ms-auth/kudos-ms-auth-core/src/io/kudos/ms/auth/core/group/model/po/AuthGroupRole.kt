package io.kudos.ms.auth.core.group.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Group-role relation database entity.
 *
 * @author K
 * @author AI: Codex
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthGroupRole : IDbEntity<String, AuthGroupRole> {

    companion object : DbEntityFactory<AuthGroupRole>()

    /** Group id. */
    var groupId: String

    /** Role id. */
    var roleId: String

    /** Who bound this role to the group (permission semantics, not an audit stamp). */
    var grantedBy: String?

    /** The upstream grant this binding was delegated from; NULL = direct. */
    var parentGrantId: String?

    /** Soft revocation: the row survives so the chain and audit trail do too. */
    var revoked: Boolean?

    /** Why the binding was revoked. */
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
