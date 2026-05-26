package io.kudos.ms.user.core.account.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * User account third-party binding database entity.
 *
 * @author K
 * @author AI: Codex
 * @since 1.0.0
 */
interface UserAccountThird : IDbEntity<String, UserAccountThird> {

    companion object : DbEntityFactory<UserAccountThird>()

    /** Associated user account id. */
    var userId: String

    /** Third-party provider dict code. */
    var accountProviderDictCode: String

    /** Issuer / provider tenant. */
    var accountProviderIssuer: String?

    /** Third-party unique user identifier. */
    var subject: String

    /** Cross-application unified identifier. */
    var unionId: String?

    /** Third-party display name. */
    var externalDisplayName: String?

    /** Third-party email. */
    var externalEmail: String?

    /** Avatar URL. */
    var avatarUrl: String?

    /** Last login time. */
    var lastLoginTime: LocalDateTime?

    /** Tenant id. */
    var tenantId: String

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
