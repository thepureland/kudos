package io.kudos.ms.auth.core.instance.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * A point-to-point grant on one instance of one resource — the "share this document with them" case.
 *
 * Kept out of the role model on purpose: RBAC answers "may this kind of person do this kind of
 * thing", and bending it to name row #4711 means a role per row.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthInstanceGrant : IDbEntity<String, AuthInstanceGrant> {

    companion object : DbEntityFactory<AuthInstanceGrant>()

    /** Who receives it. */
    var principalId: String

    /** Subject kind: USER / SERVICE / API_KEY. */
    var principalType: String?

    /** Tenant, the hard boundary as everywhere else. */
    var tenantId: String

    /** The kind of thing, e.g. `doc`. */
    var resourceType: String

    /** The particular one. */
    var instanceId: String

    /**
     * What may be done with it, written as a permission code and matched with the same wildcard
     * rules as everywhere else — `doc:read` for one action, `*` for "anything on this instance".
     */
    var action: String

    /** ALLOW or DENY; DENY wins, exactly as for role-derived grants. */
    var effect: String?

    /** Effective from; NULL = immediately. */
    var startTime: LocalDateTime?

    /** Effective until; NULL = never expires. */
    var endTime: LocalDateTime?

    /** Who shared it. */
    var grantedBy: String?

    /** Soft revocation, so an un-share stays auditable. */
    var revoked: Boolean?

    /** Why it was revoked. */
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
