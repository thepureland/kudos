package io.kudos.ms.user.core.account.model.po

import io.kudos.ability.data.rdb.ktorm.support.DbEntityFactory
import io.kudos.ability.data.rdb.ktorm.support.IDbEntity
import java.time.LocalDateTime

/**
 * User account database entity.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface UserAccount : IDbEntity<String, UserAccount> {

    companion object : DbEntityFactory<UserAccount>()

    /** Username. */
    var username: String

    /** Tenant id. */
    var tenantId: String

    /** Login password. */
    var loginPassword: String

    /** Security password. */
    var securityPassword: String?

    /** Account type dict code. */
    var accountTypeDictCode: String?

    /** Account status dict code. */
    var accountStatusDictCode: String?

    /** Default locale. */
    var defaultLocale: String?

    /** Default timezone. */
    var defaultTimezone: String?

    /** Default currency. */
    var defaultCurrency: String?

    /** Last login time. */
    var lastLoginTime: LocalDateTime?

    /** Last login IP. */
    var lastLoginIp: Long?

    /** Last logout time. */
    var lastLogoutTime: LocalDateTime?

    /** Login error count. */
    var loginErrorTimes: Int?

    /** Security-password error count. */
    var securityPasswordErrorTimes: Int?

    /** Session key. */
    var sessionKey: String?

    /** Authentication key. */
    var authenticationKey: String?

    /** Organization id. */
    var orgId: String?

    /** Direct supervisor id. */
    var supervisorId: String

    /** Remark. */
    var remark: String?

    /** Freeze type dict code; non-null indicates a freeze record exists. */
    var freezeType: String?

    /** Freeze record creation time. */
    var freezeTime: LocalDateTime?

    /** Freeze effective start time; null means effective immediately. */
    var freezeStartTime: LocalDateTime?

    /** Freeze expiration time; null means permanent freeze. */
    var freezeEndTime: LocalDateTime?

    /** Freeze reason title. */
    var freezeTitle: String?

    /** Freeze detailed description. */
    var freezeContent: String?

    /** Active flag. */
    var active: Boolean

    /** Built-in flag. */
    var builtIn: Boolean?

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
