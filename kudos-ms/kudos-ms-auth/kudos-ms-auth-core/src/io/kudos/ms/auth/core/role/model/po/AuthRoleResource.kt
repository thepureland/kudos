package io.kudos.ms.auth.core.role.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * Role-Resource relation database entity
 *
 * @author K
 * @author AI: Cursor
 * @author AI: Claude
 * @since 1.0.0
 */
interface AuthRoleResource : IDbEntity<String, AuthRoleResource> {

    companion object : DbEntityFactory<AuthRoleResource>()

    /** Role id */
    var roleId: String

    /**
     * Resource id — the legacy, environment-specific handle. Nullable because a binding may instead
     * address its permission by [permissionCode]; exactly one of the two carries the grant.
     */
    var resourceId: String?

    /** Semantic permission code (`sys:user:delete`, wildcards allowed); the durable handle. */
    var permissionCode: String?

    /** ALLOW or DENY. DENY wins over any ALLOW and cannot be overridden by a narrower rule. */
    var effect: String?

    /** Optional restricted condition expression (IP / time window / custom); NULL = unconditional. */
    var condition: String?

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
