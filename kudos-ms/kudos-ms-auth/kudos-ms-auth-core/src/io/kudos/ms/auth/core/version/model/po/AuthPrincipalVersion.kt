package io.kudos.ms.auth.core.version.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime


/**
 * A principal's token epoch — the administrative half of its permission version.
 *
 * The other half is derived from the resolved grants and is never stored: it changes because the
 * permissions changed, so there is nothing to increment. This row exists only for the part that
 * cannot be derived — "invalidate every token this principal holds, now" — which has to work even
 * when the permissions themselves did not change at all.
 *
 * @author K
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthPrincipalVersion : IDbEntity<String, AuthPrincipalVersion> {

    companion object : DbEntityFactory<AuthPrincipalVersion>()

    /** The principal this epoch belongs to; unique. */
    var principalId: String

    /** Subject kind: USER / SERVICE / API_KEY. */
    var principalType: String?

    /** Monotonic. Only ever increases, so a forced logout can never be undone by later activity. */
    var epoch: Long

    /** Why it was last bumped — credential leak, departure, forced re-authentication. */
    var reason: String?

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
