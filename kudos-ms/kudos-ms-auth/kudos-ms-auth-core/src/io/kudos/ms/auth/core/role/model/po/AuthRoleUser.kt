package io.kudos.ms.auth.core.role.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Role-User relation database entity.
 *
 * A row is **one grant**, not merely a link: besides the pair it identifies, it records who created
 * the grant, what it was delegated from, how far it may travel onwards, and whether it has been
 * revoked. See the column comments in `V1.0.0.21__init_auth_role_user.sql` for the full rationale.
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthRoleUser : IDbEntity<String, AuthRoleUser> {

    companion object : DbEntityFactory<AuthRoleUser>()

    /** Role id */
    var roleId: String

    /** User id */
    var userId: String

    /** Subject kind: USER / SERVICE / API_KEY. Machine principals share the model with people. */
    var principalType: String?

    /** Grant effective time; NULL = effective immediately. */
    var startTime: LocalDateTime?

    /** Grant expiry time; NULL = never expires. */
    var endTime: LocalDateTime?

    /** Who exercised their authority to create this grant (permission semantics, not an audit stamp). */
    var grantedBy: String?

    /** The upstream grant this was delegated from; NULL = direct grant. Revocation cascades down it. */
    var parentGrantId: String?

    /** Remaining delegation hops this grant may be passed on; 0 = the holder may not re-delegate. */
    var delegableDepth: Int?

    /** Scope frozen at grant time, so a grantor's later move cannot widen what they already handed out. */
    var scopeSnapshot: String?

    /** Soft revocation: revoked grants stay on the table so the chain and audit trail survive. */
    var revoked: Boolean?

    /** Why the grant was revoked. */
    var revokeReason: String?

    /** Creator id */
    var createUserId: String?

    /** Creator name */
    var createUserName: String?

    /** Create time */
    var createTime: LocalDateTime?

    /** Updater id */
    var updateUserId: String?

    /** Updater name */
    var updateUserName: String?

    /** Update time */
    var updateTime: LocalDateTime?




}
