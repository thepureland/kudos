package io.kudos.ms.user.core.account.service.iservice

import io.kudos.base.support.service.iservice.IBaseCrudService
import io.kudos.ms.user.common.org.vo.UserOrgCacheEntry
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.account.vo.response.AuthKeySetup
import io.kudos.ms.user.common.account.vo.response.UserAccountRow
import io.kudos.ms.user.core.account.model.po.UserAccount
import java.time.LocalDateTime


/**
 * User account service interface.
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
interface IUserAccountService : IBaseCrudService<String, UserAccount> {


    /**
     * Get the ids of all organizations the user belongs to.
     *
     * @param userId user id
     * @return List<String> organization ids, or an empty list if the user does not exist or has no organizations
     */
    fun getUserOrgIds(userId: String): List<String>

    /**
     * Get the ids of all active users under the given tenant.
     * Only ids of users with active=true are returned.
     *
     * @param tenantId tenant id
     * @return List<String> user ids
     */
    fun getUserIds(tenantId: String): List<String>



    /**
     * Get all organizations the user belongs to.
     *
     * @param userId user id
     * @return List<UserOrgCacheEntry> organizations, or an empty list if the user does not exist or has none
     */
    fun getUserOrgs(userId: String): List<UserOrgCacheEntry>



    /**
     * Check whether a user belongs to a given organization.
     *
     * @param userId user id
     * @param orgId organization id
     * @return true if the user belongs to the organization, false otherwise
     */
    fun isUserInOrg(userId: String, orgId: String): Boolean



    /**
     * Get user info by tenant id and username.
     *
     * @param tenantId tenant id
     * @param username username
     * @return user cache entry, or null if not found
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserByTenantIdAndUsername(tenantId: String, username: String): UserAccountCacheEntry?

    /**
     * Get a user record by id (bypasses cache).
     *
     * @param id user id
     * @return user record, or null if not found
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUserRecord(id: String): UserAccountRow?

    /**
     * Get the user list under the given tenant.
     *
     * @param tenantId tenant id
     * @return list of user records
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByTenantId(tenantId: String): List<UserAccountRow>

    /**
     * Get the user list under the given organization.
     *
     * @param orgId organization id
     * @return list of user records
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun getUsersByOrgId(orgId: String): List<UserAccountRow>



    /**
     * Update the user's active flag.
     *
     * @param id user id
     * @param active whether the user is active
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateActive(id: String, active: Boolean): Boolean

    /**
     * Reset the login password.
     *
     * @param id user id
     * @param newPassword new password (plain text)
     * @return whether the reset succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetPassword(id: String, newPassword: String): Boolean

    /**
     * Reset the security password.
     *
     * @param id user id
     * @param newPassword new password (plain text)
     * @return whether the reset succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetSecurityPassword(id: String, newPassword: String): Boolean

    /**
     * Update last-login info.
     *
     * @param id user id
     * @param loginIp login IP
     * @param loginTime login time
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateLastLoginInfo(id: String, loginIp: Long, loginTime: LocalDateTime): Boolean

    /**
     * Update last-logout info.
     *
     * @param id user id
     * @param logoutTime logout time
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun updateLastLogoutInfo(id: String, logoutTime: LocalDateTime): Boolean

    /**
     * Increment the login error count.
     *
     * @param id user id
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun incrementLoginErrorTimes(id: String): Boolean

    /**
     * Reset the login error count.
     *
     * @param id user id
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetLoginErrorTimes(id: String): Boolean

    /**
     * Increment the security-password error count.
     *
     * @param id user id
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun incrementSecurityPasswordErrorTimes(id: String): Boolean

    /**
     * Reset the security-password error count.
     *
     * @param id user id
     * @return whether the update succeeded
     * @author AI: Cursor
     * @since 1.0.0
     */
    fun resetSecurityPasswordErrorTimes(id: String): Boolean

    /**
     * Generate a new TOTP secret for the user, persist it (overwriting the old value),
     * and return the secret plus an otpauth URL.
     *
     * After the user scans it in an OTP app (e.g. Google Authenticator), two-factor auth is enabled.
     *
     * @param id user primary key
     * @param accountName account display name shown in the OTP app (typically the username)
     * @param issuer issuer display name shown in the OTP app (typically the application name, e.g. "kudos")
     * @return [AuthKeySetup] containing secret + otpauthUrl; null when the user is missing or the DB write fails
     */
    fun resetAuthKey(id: String, accountName: String, issuer: String): AuthKeySetup?

    /**
     * Clear the user's TOTP secret (disable two-factor auth).
     *
     * @param id user primary key
     * @return whether the update succeeded
     */
    fun cleanAuthKey(id: String): Boolean

    /**
     * Verify a 6-digit TOTP code submitted by the user.
     *
     * @param id user primary key
     * @param code the 6-digit number currently shown by the user's OTP app (leading zeros may be dropped, hence Long)
     * @return true on match; false on mismatch or when OTP is not enabled for the user
     */
    fun verifyAuthCode(id: String, code: Long): Boolean

    /**
     * Freeze an account: write the 6 freeze_* columns. `freeze_time` is set by the implementation to [LocalDateTime.now].
     *
     * Login logic: when freeze_type IS NOT NULL and
     * (freeze_start_time IS NULL or now >= freeze_start_time) and
     * (freeze_end_time IS NULL or now < freeze_end_time), the account is considered "currently frozen" and login is denied.
     *
     * @param id user primary key
     * @param freezeType freeze type dict code (manual / auto / admin / scheduled, etc.)
     * @param freezeTitle short title; nullable
     * @param freezeContent detailed description; nullable
     * @param freezeStartTime effective start time; null = effective immediately
     * @param freezeEndTime expiration time; null = permanent freeze
     * @return whether the update succeeded
     */
    fun freezeAccount(
        id: String,
        freezeType: String,
        freezeTitle: String?,
        freezeContent: String?,
        freezeStartTime: LocalDateTime?,
        freezeEndTime: LocalDateTime?,
    ): Boolean

    /**
     * Unfreeze an account: clear all 6 freeze_* columns.
     *
     * @param id user primary key
     * @return whether the update succeeded
     */
    fun unfreezeAccount(id: String): Boolean

    /**
     * Scan and clean expired freeze records: `freeze_end_time IS NOT NULL AND freeze_end_time < now()`.
     *
     * Note: the login logic itself already lets through "frozen outside the active window"
     * (see `PassportService.isCurrentlyFrozen`), so this method is **cleanup only** and does not affect
     * functional correctness. Its value is to:
     *   - keep the admin list clean (no more "expired but still flagged as frozen" dirty data)
     *   - avoid the freeze_* fields lingering in the cache with meaningless data
     *
     * The scheduling strategy is up to the caller -- it can run automatically via `AutoUnfreezeScheduler`
     * (provided the consumer enables `@EnableScheduling`) or be triggered manually from the admin side.
     *
     * @return number of accounts cleaned up
     */
    fun cleanExpiredFreezes(): Int


}
