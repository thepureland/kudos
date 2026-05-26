package io.kudos.ms.user.core.account.model.table

import io.kudos.ability.data.rdb.ktorm.support.StringIdTable
import io.kudos.ms.user.core.account.model.po.UserAccount
import org.ktorm.schema.*


/**
 * User account table-entity binding object
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
object UserAccounts : StringIdTable<UserAccount>("user_account") {

    /** Username */
    var username = varchar("username").bindTo { it.username }

    /** Tenant ID */
    var tenantId = varchar("tenant_id").bindTo { it.tenantId }

    /** Login password */
    var loginPassword = varchar("login_password").bindTo { it.loginPassword }

    /** Security password */
    var securityPassword = varchar("security_password").bindTo { it.securityPassword }

    /** Account type dict code */
    var accountTypeDictCode = varchar("account_type_dict_code").bindTo { it.accountTypeDictCode }

    /** Account status dict code */
    var accountStatusDictCode = varchar("account_status_dict_code").bindTo { it.accountStatusDictCode }

    /** Default locale */
    var defaultLocale = varchar("default_locale").bindTo { it.defaultLocale }

    /** Default timezone */
    var defaultTimezone = varchar("default_timezone").bindTo { it.defaultTimezone }

    /** Default currency */
    var defaultCurrency = varchar("default_currency").bindTo { it.defaultCurrency }

    /** Last login time */
    var lastLoginTime = datetime("last_login_time").bindTo { it.lastLoginTime }

    /** Last login IP */
    var lastLoginIp = long("last_login_ip").bindTo { it.lastLoginIp }

    /** Last logout time */
    var lastLogoutTime = datetime("last_logout_time").bindTo { it.lastLogoutTime }

    /** Login error count */
    var loginErrorTimes = int("login_error_times").bindTo { it.loginErrorTimes }

    /** Security password error count */
    var securityPasswordErrorTimes = int("security_password_error_times").bindTo { it.securityPasswordErrorTimes }

    /** Session key */
    var sessionKey = varchar("session_key").bindTo { it.sessionKey }

    /** Authentication key */
    var authenticationKey = varchar("authentication_key").bindTo { it.authenticationKey }

    /** Owning org ID */
    var orgId = varchar("org_id").bindTo { it.orgId }

    /** Direct supervisor ID */
    var supervisorId = varchar("supervisor_id").bindTo { it.supervisorId }

    /** Remark */
    var remark = varchar("remark").bindTo { it.remark }

    /** Freeze type dict code */
    var freezeType = varchar("freeze_type").bindTo { it.freezeType }

    /** Time the freeze record was created */
    var freezeTime = datetime("freeze_time").bindTo { it.freezeTime }

    /** Freeze effective start */
    var freezeStartTime = datetime("freeze_start_time").bindTo { it.freezeStartTime }

    /** Freeze expiration time */
    var freezeEndTime = datetime("freeze_end_time").bindTo { it.freezeEndTime }

    /** Freeze reason title */
    var freezeTitle = varchar("freeze_title").bindTo { it.freezeTitle }

    /** Freeze detailed description */
    var freezeContent = varchar("freeze_content").bindTo { it.freezeContent }

    /** Whether active */
    var active = boolean("active").bindTo { it.active }

    /** Whether built-in */
    var builtIn = boolean("built_in").bindTo { it.builtIn }

    /** Creator id */
    var createUserId = varchar("create_user_id").bindTo { it.createUserId }

    /** Creator name */
    var createUserName = varchar("create_user_name").bindTo { it.createUserName }

    /** Create time */
    var createTime = datetime("create_time").bindTo { it.createTime }

    /** Updater id */
    var updateUserId = varchar("update_user_id").bindTo { it.updateUserId }

    /** Updater name */
    var updateUserName = varchar("update_user_name").bindTo { it.updateUserName }

    /** Update time */
    var updateTime = datetime("update_time").bindTo { it.updateTime }




}
