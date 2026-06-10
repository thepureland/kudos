package io.kudos.ms.user.core.passport.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.security.GoogleAuthenticator
import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * Login passport business implementation.
 *
 * Flow:
 *   1) Fetch the user cache entry by (tenantId, username).
 *   2) If it does not exist -> USER_NOT_FOUND.
 *   3) If active != true -> INACTIVE.
 *   4) Freeze gate: a currently effective freeze rejects the attempt
 *      (LOCKED for the automatic login lock, ACCOUNT_FROZEN otherwise).
 *   5) BCrypt-verify the plaintext password.
 *      - On failure -> incrementLoginErrorTimes; once the accumulated count reaches the
 *        configurable threshold the account is auto-locked (see below) and LOCKED is returned,
 *        otherwise WRONG_PASSWORD + current error count.
 *   6) If authentication_key is not empty (OTP enabled):
 *      - Request does not carry authCode -> return OTP_REQUIRED (does not touch the error count).
 *      - authCode fails GoogleAuthenticator verification -> same failure handling as a wrong password.
 *   7) All passes -> resetLoginErrorTimes + updateLastLoginInfo (if IP is present), return SUCCESS + UserInfoModel.
 *
 * Brute-force lockout:
 *   When the accumulated login error count reaches `kudos.ms.user.passport.login-lock.max-error-times`
 *   (default 5, non-positive disables the gate), the account is frozen with the dedicated freeze type
 *   [LOGIN_LOCK_FREEZE_TYPE] for `kudos.ms.user.passport.login-lock.lock-minutes` minutes
 *   (default 30, non-positive means locked until manual intervention). The freeze reuses the existing
 *   account-freeze machinery, so the lock expires automatically (AutoUnfreezeScheduler cleans the
 *   record) and a successful login resets the error count. An existing freeze of a different type
 *   (manual, admin, ...) is never overwritten by the auto lock.
 *
 * @author K
 * @since 1.0.0
 */
@Service
@Transactional
open class PassportService(
    private val userAccountService: IUserAccountService,
    private val userAccountDao: UserAccountDao,
) : IPassportService {

    /** Consecutive login failures that trigger the auto lock; non-positive disables the lockout gate. */
    @Value($$"${kudos.ms.user.passport.login-lock.max-error-times:5}")
    protected var maxLoginErrorTimes: Int = 5

    /** Auto-lock window in minutes; non-positive means locked until manual unfreeze or password reset. */
    @Value($$"${kudos.ms.user.passport.login-lock.lock-minutes:30}")
    protected var loginLockMinutes: Long = 30

    private val log = LogFactory.getLog(this::class)

    override fun login(req: PassportLoginRequest): PassportLoginResult {
        val user = userAccountService.getUserByTenantIdAndUsername(req.tenantId, req.username)
            ?: run {
                log.debug("Login failed - user not found: tenantId=${req.tenantId} username=${req.username}")
                return PassportLoginResult.userNotFound()
            }

        if (user.active != true) {
            log.debug("Login failed - account not enabled: userId=${user.id}")
            return PassportLoginResult.inactive()
        }

        // Freeze check: freeze_type is not null + currently within the effective window.
        if (isCurrentlyFrozen(user.freezeType, user.freezeStartTime, user.freezeEndTime)) {
            if (user.freezeType == LOGIN_LOCK_FREEZE_TYPE) {
                log.debug(
                    "Login rejected - account auto-locked after repeated failures: userId=${user.id} " +
                        "window=[${user.freezeStartTime}, ${user.freezeEndTime})"
                )
                return PassportLoginResult.locked(user.loginErrorTimes)
            }
            log.debug(
                "Login failed - account frozen: userId=${user.id} type=${user.freezeType} " +
                    "window=[${user.freezeStartTime}, ${user.freezeEndTime})"
            )
            return PassportLoginResult.accountFrozen(user.freezeTitle)
        }

        val passwordMatches = PasswordKit.matches(req.plainPassword, user.loginPassword)
        if (!passwordMatches) {
            val (accumulated, locked) = registerLoginFailure(user)
            if (locked) return PassportLoginResult.locked(accumulated)
            log.debug("Login failed - wrong password: userId=${user.id} accumulated error count=${accumulated}")
            return PassportLoginResult.wrongPassword(accumulated)
        }

        // Password correct, check whether OTP secondary verification is enabled.
        val authKey = user.authenticationKey
        if (!authKey.isNullOrBlank()) {
            val authCode = req.authCode
            if (authCode == null) {
                log.debug("Login pending - OTP required: userId=${user.id}")
                return PassportLoginResult.otpRequired()
            }
            val otpOk = GoogleAuthenticator().checkCode(authKey, authCode, System.currentTimeMillis())
            if (!otpOk) {
                val (accumulated, locked) = registerLoginFailure(user)
                if (locked) return PassportLoginResult.locked(accumulated)
                log.debug("Login failed - OTP wrong: userId=${user.id} accumulated error count=${accumulated}")
                return PassportLoginResult.otpWrong(accumulated)
            }
        }

        // All verifications pass: reset the error count + record last login info.
        userAccountService.resetLoginErrorTimes(user.id)
        val now = LocalDateTime.now()
        req.loginIp?.let { userAccountService.updateLastLoginInfo(user.id, it, now) }

        log.debug("Login succeeded: userId=${user.id} username=${user.username}")
        return PassportLoginResult.success(
            UserInfoModel(
                id = user.id,
                username = user.username.orEmpty(),
                tenantId = user.tenantId.orEmpty(),
                orgId = user.orgId,
                accountTypeDictCode = user.accountTypeDictCode,
                defaultLocale = user.defaultLocale,
                defaultTimezone = user.defaultTimezone,
                defaultCurrency = user.defaultCurrency,
                loginTime = now,
            )
        )
    }

    /**
     * Record one login failure: increment the error counter and, when the accumulated count
     * reaches the configured threshold, arm the automatic login lock by freezing the account
     * with [LOGIN_LOCK_FREEZE_TYPE] for [loginLockMinutes] minutes.
     *
     * An existing freeze of a different type is never overwritten (it would already reject the
     * login at the freeze gate once effective); re-arming an expired auto lock is allowed so that
     * a still-elevated counter immediately re-locks on the next failure.
     *
     * @param user the cache entry of the account that failed verification
     * @return accumulated error count (after this failure) and whether the account is now locked
     */
    private fun registerLoginFailure(user: UserAccountCacheEntry): Pair<Int, Boolean> {
        userAccountService.incrementLoginErrorTimes(user.id)
        // Use the Row queried directly via DAO to get the actual count after the increment;
        // do not rely on the cache (the cache may lag due to the event not being committed).
        val accumulated = userAccountService.getUserRecord(user.id)?.loginErrorTimes
            ?: ((user.loginErrorTimes ?: 0) + 1)
        val lock = shouldLockLogin(accumulated, maxLoginErrorTimes)
        if (lock && canArmLoginLock(user.freezeType)) {
            val lockEnd = if (loginLockMinutes > 0) LocalDateTime.now().plusMinutes(loginLockMinutes) else null
            userAccountService.freezeAccount(
                id = user.id,
                freezeType = LOGIN_LOCK_FREEZE_TYPE,
                freezeTitle = "Account locked after too many failed login attempts",
                freezeContent = "Auto-locked after ${accumulated} consecutive login failures" +
                    (lockEnd?.let { "; unlocks automatically at $it" } ?: "; requires manual unfreeze"),
                freezeStartTime = null,
                freezeEndTime = lockEnd,
            )
            log.warn(
                "Login lock armed: userId=${user.id} accumulated error count=${accumulated} " +
                    "lock end=${lockEnd ?: "manual intervention required"}"
            )
        }
        return accumulated to lock
    }

    override fun logout(userId: String): Boolean {
        val success = userAccountService.updateLastLogoutInfo(userId, LocalDateTime.now())
        if (success) log.debug("Logout succeeded: userId=${userId}")
        else log.debug("Logout failed (user does not exist?): userId=${userId}")
        return success
    }

    @Transactional(readOnly = true)
    override fun verifyPassword(req: VerifyPasswordRequest): Boolean {
        // Query the DAO directly: get the latest loginPassword hash (does not go through cache to avoid lagging behind changePassword write).
        val storedHash = userAccountDao.get(req.userId)?.loginPassword ?: return false
        return PasswordKit.matches(req.plainPassword, storedHash)
    }

    @Transactional(readOnly = true)
    override fun verifySecurityPassword(req: VerifyPasswordRequest): Boolean {
        val storedHash = userAccountDao.get(req.userId)?.securityPassword ?: return false
        return PasswordKit.matches(req.plainPassword, storedHash)
    }

    override fun changePassword(req: ChangePasswordRequest): ChangePasswordResultEnum {
        val po = userAccountDao.get(req.userId)
            ?: return ChangePasswordResultEnum.USER_NOT_FOUND
        if (!PasswordKit.matches(req.oldPlainPassword, po.loginPassword)) {
            return ChangePasswordResultEnum.OLD_PASSWORD_WRONG
        }
        // Old password correct -- directly call the existing resetPassword (it will hash the new password and reset the error count).
        userAccountService.resetPassword(req.userId, req.newPlainPassword)
        return ChangePasswordResultEnum.SUCCESS
    }

    override fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum {
        val po = userAccountDao.get(req.userId)
            ?: return ChangePasswordResultEnum.USER_NOT_FOUND
        if (!PasswordKit.matches(req.oldPlainPassword, po.securityPassword)) {
            return ChangePasswordResultEnum.OLD_PASSWORD_WRONG
        }
        userAccountService.resetSecurityPassword(req.userId, req.newPlainPassword)
        return ChangePasswordResultEnum.SUCCESS
    }

    companion object {

        /**
         * Dedicated freeze type code for the automatic brute-force login lock.
         *
         * Distinct from manually managed codes (manual, admin, scheduled, ...) so the freeze gate
         * can answer LOCKED instead of ACCOUNT_FROZEN, and so the lock never masquerades as an
         * administrative freeze in audit views.
         */
        const val LOGIN_LOCK_FREEZE_TYPE = "autoLoginLock"

        /**
         * Whether the accumulated login error count has reached the lockout threshold.
         *
         * A non-positive [maxErrorTimes] disables the lockout gate entirely.
         *
         * Pure function and `internal` so the threshold logic can be unit-tested directly
         * without standing up the full login pipeline.
         *
         * @param accumulatedErrorTimes error count including the current failure
         * @param maxErrorTimes configured threshold
         * @return true when the account must be locked
         */
        internal fun shouldLockLogin(accumulatedErrorTimes: Int, maxErrorTimes: Int): Boolean =
            maxErrorTimes > 0 && accumulatedErrorTimes >= maxErrorTimes

        /**
         * Whether the automatic login lock may (re-)freeze the account.
         *
         * Allowed when there is no freeze record at all, or when the existing record is our own
         * auto lock (re-arming an expired lock window). A freeze of any other type (manual, admin,
         * scheduled, ...) is left untouched so the auto lock never clobbers an administrative decision.
         *
         * Pure function and `internal` for direct unit testing.
         *
         * @param existingFreezeType the account's current freeze type code, may be null or blank
         * @return true when freezing with [LOGIN_LOCK_FREEZE_TYPE] is permitted
         */
        internal fun canArmLoginLock(existingFreezeType: String?): Boolean =
            existingFreezeType.isNullOrBlank() || existingFreezeType == LOGIN_LOCK_FREEZE_TYPE

        /**
         * Whether the current moment falls within the freeze effective window.
         *
         * - freezeType is null/empty -> no freeze record, always false.
         * - freezeStartTime is null -> treated as "immediately effective", the lower bound is always satisfied.
         * - freezeEndTime is null -> treated as "permanently frozen", the upper bound is always satisfied.
         *
         * Pure function (no instance state) and `internal` so it can be unit-tested directly without
         * standing up the full login pipeline.
         */
        internal fun isCurrentlyFrozen(
            freezeType: String?,
            freezeStartTime: LocalDateTime?,
            freezeEndTime: LocalDateTime?,
        ): Boolean {
            if (freezeType.isNullOrBlank()) return false
            val now = LocalDateTime.now()
            val afterStart = freezeStartTime == null || !now.isBefore(freezeStartTime)
            val beforeEnd = freezeEndTime == null || now.isBefore(freezeEndTime)
            return afterStart && beforeEnd
        }
    }
}
