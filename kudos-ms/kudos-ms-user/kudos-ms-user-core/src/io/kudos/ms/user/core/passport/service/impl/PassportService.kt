package io.kudos.ms.user.core.passport.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.security.GoogleAuthenticator
import io.kudos.ability.security.common.support.PasswordEncodingKit
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.account.vo.UserAccountCacheEntry
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.account.security.IAccountCredentialStore
import io.kudos.ms.user.core.account.security.PasswordPolicyContext
import io.kudos.ms.user.core.account.security.PasswordPurpose
import io.kudos.ms.user.core.account.security.PasswordPolicyException
import io.kudos.ms.user.core.account.security.PasswordReusedException
import io.kudos.ms.user.core.login.model.UserLoginAttempt
import io.kudos.ms.user.core.login.service.iservice.IUserLogLoginService
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptContext
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptDecision
import io.kudos.ms.user.core.passport.security.AuthenticationAttemptFactorEnum
import io.kudos.ms.user.core.passport.security.IAuthenticationAttemptLimiter
import io.kudos.ms.user.core.passport.security.NoopAuthenticationAttemptLimiter
import io.kudos.ms.user.core.passport.security.IRecoveryCodeVerifier
import io.kudos.ms.user.core.passport.security.NoopRecoveryCodeVerifier
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import org.springframework.beans.factory.annotation.Value
import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDateTime


/**
 * Login passport business implementation.
 *
 * Flow:
 *   1) Consume the server IP and tenant/principal request-rate buckets.
 *   2) Fetch the user cache entry by (tenantId, username); unknown principals consume the password-failure bucket
 *      and one dummy BCrypt verification to reduce user-enumeration timing differences.
 *   3) If active != true -> INACTIVE.
 *   4) Freeze gate: a currently effective freeze rejects the attempt
 *      (LOCKED for the automatic login lock, ACCOUNT_FROZEN otherwise).
 *   5) Check the password failure window, then BCrypt-verify the plaintext password.
 *      - On failure -> incrementLoginErrorTimes; once the accumulated count reaches the
 *        configurable threshold the account is auto-locked (see below) and LOCKED is returned,
 *        otherwise WRONG_PASSWORD + current error count.
 *   6) If authentication_key is not empty (OTP enabled):
 *      - Request does not carry authCode -> return OTP_REQUIRED (does not touch the error count).
 *      - Check the separate TOTP failure window; a wrong code consumes only that window.
 *   7) All passes -> clear factor-failure buckets, resetLoginErrorTimes, updateLastLoginInfo (if IP is present),
 *      and return SUCCESS + UserInfoModel.
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
    private val userLogLoginService: IUserLogLoginService,
    private val passwordEncoder: PasswordEncoder,
    private val authenticationAttemptLimiter: IAuthenticationAttemptLimiter = NoopAuthenticationAttemptLimiter,
    private val recoveryCodeVerifier: IRecoveryCodeVerifier = NoopRecoveryCodeVerifier,
    private val credentialStores: List<IAccountCredentialStore> = emptyList(),
) : IPassportService {

    /** The credential owner when one is deployed; otherwise the login password is still in the column. */
    private val credentialStore: IAccountCredentialStore?
        get() = credentialStores.firstOrNull()

    /** Consecutive login failures that trigger the auto lock; non-positive disables the lockout gate. */
    @Value($$"${kudos.ms.user.passport.login-lock.max-error-times:5}")
    protected var maxLoginErrorTimes: Int = 5

    /** Auto-lock window in minutes; non-positive means locked until manual unfreeze or password reset. */
    @Value($$"${kudos.ms.user.passport.login-lock.lock-minutes:30}")
    protected var loginLockMinutes: Long = 30

    private val log = LogFactory.getLog(this::class)

    override fun login(req: PassportLoginRequest): PassportLoginResult {
        val attemptStartedAt = LocalDateTime.now()
        val attemptContext = AuthenticationAttemptContext(req.tenantId, req.username, req.loginIp)
        fun finish(userId: String?, result: PassportLoginResult): PassportLoginResult {
            recordLoginAttempt(req, userId, result.status, attemptStartedAt)
            return result
        }

        authenticationAttemptLimiter.consumeRequest(attemptContext).takeUnless { it.allowed }?.let { decision ->
            return finish(null, rateLimited(decision))
        }
        // Apply the factor gate before account lookup/status checks so rate-limit behavior cannot
        // reveal whether the principal exists or is currently usable.
        authenticationAttemptLimiter.checkFailureLimit(
            attemptContext,
            AuthenticationAttemptFactorEnum.PASSWORD,
        ).takeUnless { it.allowed }?.let { decision ->
            return finish(null, rateLimited(decision))
        }

        val user = userAccountService.getUserByTenantIdAndUsername(req.tenantId, req.username)
            ?: run {
                consumePasswordVerificationCost(req.plainPassword, null)
                authenticationAttemptLimiter.recordFailure(
                    attemptContext,
                    AuthenticationAttemptFactorEnum.PASSWORD,
                )
                log.debug("Login failed - user not found: tenantId=${req.tenantId} username=${req.username}")
                return finish(null, PassportLoginResult.userNotFound())
            }

        if (user.active != true) {
            consumePasswordVerificationCost(req.plainPassword, user.loginPassword)
            log.debug("Login failed - account not enabled: userId=${user.id}")
            return finish(user.id, PassportLoginResult.inactive())
        }

        // Freeze check: freeze_type is not null + currently within the effective window.
        if (isCurrentlyFrozen(user.freezeType, user.freezeStartTime, user.freezeEndTime)) {
            consumePasswordVerificationCost(req.plainPassword, user.loginPassword)
            if (user.freezeType == LOGIN_LOCK_FREEZE_TYPE) {
                log.debug(
                    "Login rejected - account auto-locked after repeated failures: userId=${user.id} " +
                        "window=[${user.freezeStartTime}, ${user.freezeEndTime})"
                )
                return finish(user.id, PassportLoginResult.locked(user.loginErrorTimes))
            }
            log.debug(
                "Login failed - account frozen: userId=${user.id} type=${user.freezeType} " +
                    "window=[${user.freezeStartTime}, ${user.freezeEndTime})"
            )
            return finish(user.id, PassportLoginResult.accountFrozen(user.freezeTitle))
        }

        val passwordMatches =
            loginPasswordMatches(user.id, req.tenantId, req.username, req.plainPassword, user.loginPassword)
        if (!passwordMatches) {
            authenticationAttemptLimiter.recordFailure(
                attemptContext,
                AuthenticationAttemptFactorEnum.PASSWORD,
            )
            val (accumulated, locked) = registerLoginFailure(user)
            if (locked) return finish(user.id, PassportLoginResult.locked(accumulated))
            log.debug("Login failed - wrong password: userId=${user.id} accumulated error count=${accumulated}")
            return finish(user.id, PassportLoginResult.wrongPassword(accumulated))
        }
        authenticationAttemptLimiter.clearFailures(
            attemptContext,
            setOf(AuthenticationAttemptFactorEnum.PASSWORD),
        )

        // Password correct, check whether OTP secondary verification is enabled.
        val authKey = user.authenticationKey
        if (!authKey.isNullOrBlank()) {
            val authCode = req.authCode
            val recoveryCode = req.recoveryCode?.trim()?.takeIf(String::isNotEmpty)
            if (authCode == null && recoveryCode == null) {
                log.debug("Login pending - OTP required: userId=${user.id}")
                // This is a challenge continuation, not a terminal login outcome. Logging it would
                // over-count a single password + OTP flow as two attempts.
                return PassportLoginResult.otpRequired()
            }
            if (recoveryCode != null && authCode == null) {
                authenticationAttemptLimiter.checkFailureLimit(
                    attemptContext,
                    AuthenticationAttemptFactorEnum.RECOVERY_CODE,
                ).takeUnless { it.allowed }?.let { decision ->
                    return finish(user.id, rateLimited(decision))
                }
                if (!recoveryCodeVerifier.consumeRecoveryCode(req.tenantId, user.id, recoveryCode)) {
                    authenticationAttemptLimiter.recordFailure(
                        attemptContext,
                        AuthenticationAttemptFactorEnum.RECOVERY_CODE,
                    )
                    log.debug("Login failed - recovery code wrong: userId=${user.id}")
                    return finish(user.id, PassportLoginResult.recoveryCodeWrong())
                }
                authenticationAttemptLimiter.clearFailures(
                    attemptContext,
                    setOf(AuthenticationAttemptFactorEnum.RECOVERY_CODE),
                )
            } else {
                authenticationAttemptLimiter.checkFailureLimit(
                    attemptContext,
                    AuthenticationAttemptFactorEnum.TOTP,
                ).takeUnless { it.allowed }?.let { decision ->
                    return finish(user.id, rateLimited(decision))
                }
                val otpOk = GoogleAuthenticator().checkCode(authKey, requireNotNull(authCode), System.currentTimeMillis())
                if (!otpOk) {
                    authenticationAttemptLimiter.recordFailure(
                        attemptContext,
                        AuthenticationAttemptFactorEnum.TOTP,
                    )
                    log.debug("Login failed - OTP wrong: userId=${user.id}")
                    return finish(user.id, PassportLoginResult.otpWrong())
                }
            }
        }

        // Upgrade only after every factor succeeds. A failed/racing upgrade must never alter this
        // authentication result, and the service CAS prevents overwriting a concurrent password change.
        upgradePasswordEncodingIfNeeded(
            user.id, req.tenantId, req.username, req.plainPassword, user.loginPassword,
        )

        // All verifications pass: reset the error count + record last login info.
        authenticationAttemptLimiter.clearFailures(attemptContext)
        userAccountService.resetLoginErrorTimes(user.id)
        val now = LocalDateTime.now()
        req.loginIp?.let { userAccountService.updateLastLoginInfo(user.id, it, now) }

        log.debug("Login succeeded: userId=${user.id} username=${user.username}")
        return finish(user.id, PassportLoginResult.success(
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
        ))
    }

    /**
     * Persist a terminal result without allowing audit infrastructure failure to change the
     * authentication result. [IUserLogLoginService] runs the insert in an independent transaction.
     */
    private fun recordLoginAttempt(
        req: PassportLoginRequest,
        userId: String?,
        status: PassportLoginStatusEnum,
        attemptStartedAt: LocalDateTime,
    ) {
        try {
            userLogLoginService.recordLoginAttempt(
                UserLoginAttempt(
                    userId = userId,
                    username = req.username,
                    tenantId = req.tenantId,
                    loginTime = attemptStartedAt,
                    loginIp = req.loginIp,
                    loginDevice = req.loginDevice,
                    loginBrowser = req.loginBrowser,
                    loginOs = req.loginOs,
                    userAgent = req.userAgent,
                    loginSuccess = status == PassportLoginStatusEnum.SUCCESS,
                    failureReason = status.takeUnless { it == PassportLoginStatusEnum.SUCCESS }?.name,
                )
            )
        } catch (e: Exception) {
            log.error(
                e,
                "Failed to persist login audit: tenantId=${req.tenantId} username=${req.username} status=$status"
            )
        }
    }

    private fun rateLimited(decision: AuthenticationAttemptDecision): PassportLoginResult =
        PassportLoginResult.rateLimited(decision.retryAfterSeconds)

    /**
     * Perform the same expensive primitive used by the normal password path before returning from
     * account-revealing lookup/status branches. A malformed or missing stored hash uses a fixed
     * BCrypt hash so credential data quality cannot recreate the fast path.
     *
     * This mitigates the obvious timing oracle; infrastructure and historical BCrypt cost factors
     * can still introduce noise, so the public response contract remains the primary protection.
     *
     * With a credential store deployed the column is empty and every call here burns the fixed hash. That is
     * deliberate rather than an oversight: the branch where no account was found cannot consult the store at
     * all, so having the other branches do so would put a database read on exactly the side that reveals the
     * account exists.
     */
    private fun consumePasswordVerificationCost(plainPassword: String, storedHash: String?) {
        val verificationHash = storedHash.takeIf(PasswordEncodingKit::looksLikeEncodedPassword)
            ?: DUMMY_PASSWORD_HASH
        PasswordEncodingKit.matches(passwordEncoder, plainPassword, verificationHash)
    }

    /**
     * Verifies the login password against whichever side owns it.
     *
     * [storedHash] is the column, which a deployment that owns credentials elsewhere leaves empty — checking
     * it there would fail every login, so the store is asked first whenever one is present.
     */
    private fun loginPasswordMatches(
        userId: String,
        tenantId: String?,
        username: String?,
        plainPassword: String,
        storedHash: String?,
    ): Boolean {
        val store = credentialStore
            ?: return PasswordEncodingKit.matches(passwordEncoder, plainPassword, storedHash)
        return store.verifyPassword(plainPassword, loginContext(userId, tenantId, username))
    }

    private fun loginContext(userId: String, tenantId: String?, username: String?) =
        PasswordPolicyContext(PasswordPurpose.LOGIN, userId, username, tenantId)

    private fun upgradePasswordEncodingIfNeeded(
        userId: String,
        tenantId: String?,
        username: String?,
        plainPassword: String,
        storedHash: String?,
    ) {
        val store = credentialStore
        if (store != null) {
            // Deciding and replacing both happen inside the store, under the credential's version.
            runCatching { store.upgradePasswordEncodingIfNeeded(plainPassword, loginContext(userId, tenantId, username)) }
                .onFailure { e ->
                    log.warn("Failed to upgrade login-password encoding; authentication continues: userId=$userId", e)
                }
            return
        }
        if (!PasswordEncodingKit.upgradeEncoding(passwordEncoder, storedHash)) return
        val expectedHash = storedHash ?: return
        try {
            val upgradedHash = requireNotNull(passwordEncoder.encode(plainPassword)) {
                "Password encoder returned null"
            }
            if (!userAccountService.upgradeLoginPasswordEncoding(userId, expectedHash, upgradedHash)) {
                log.debug("Skipped login-password encoding upgrade after a concurrent update: userId=$userId")
            }
        } catch (e: Exception) {
            log.warn("Failed to upgrade login-password encoding; authentication continues: userId=$userId", e)
        }
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
        val account = userAccountDao.get(req.userId) ?: return false
        return loginPasswordMatches(
            account.id, account.tenantId, account.username, req.plainPassword, account.loginPassword,
        )
    }

    @Transactional(readOnly = true)
    override fun verifySecurityPassword(req: VerifyPasswordRequest): Boolean {
        val storedHash = userAccountDao.get(req.userId)?.securityPassword ?: return false
        return PasswordEncodingKit.matches(passwordEncoder, req.plainPassword, storedHash)
    }

    override fun changePassword(req: ChangePasswordRequest): ChangePasswordResultEnum {
        val po = userAccountDao.get(req.userId)
            ?: return ChangePasswordResultEnum.USER_NOT_FOUND
        if (!loginPasswordMatches(po.id, po.tenantId, po.username, req.oldPlainPassword, po.loginPassword)) {
            return ChangePasswordResultEnum.OLD_PASSWORD_WRONG
        }
        if (loginPasswordMatches(po.id, po.tenantId, po.username, req.newPlainPassword, po.loginPassword)) {
            return ChangePasswordResultEnum.PASSWORD_REUSED
        }
        // Old password correct -- directly call the existing resetPassword (it will hash the new password and reset the error count).
        return try {
            if (userAccountService.resetPassword(req.userId, req.newPlainPassword)) {
                ChangePasswordResultEnum.SUCCESS
            } else {
                ChangePasswordResultEnum.USER_NOT_FOUND
            }
        } catch (_: PasswordReusedException) {
            ChangePasswordResultEnum.PASSWORD_REUSED
        } catch (_: PasswordPolicyException) {
            ChangePasswordResultEnum.PASSWORD_POLICY_VIOLATION
        }
    }

    override fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum {
        val po = userAccountDao.get(req.userId)
            ?: return ChangePasswordResultEnum.USER_NOT_FOUND
        if (!PasswordEncodingKit.matches(passwordEncoder, req.oldPlainPassword, po.securityPassword)) {
            return ChangePasswordResultEnum.OLD_PASSWORD_WRONG
        }
        if (PasswordEncodingKit.matches(passwordEncoder, req.newPlainPassword, po.securityPassword)) {
            return ChangePasswordResultEnum.PASSWORD_REUSED
        }
        return try {
            if (userAccountService.resetSecurityPassword(req.userId, req.newPlainPassword)) {
                ChangePasswordResultEnum.SUCCESS
            } else {
                ChangePasswordResultEnum.USER_NOT_FOUND
            }
        } catch (_: PasswordReusedException) {
            ChangePasswordResultEnum.PASSWORD_REUSED
        } catch (_: PasswordPolicyException) {
            ChangePasswordResultEnum.PASSWORD_POLICY_VIOLATION
        }
    }

    companion object {

        /** Fixed BCrypt cost-10 hash used only for timing equalization; it is not a credential. */
        private const val DUMMY_PASSWORD_HASH =
            "\$2a\$10\$7EqJtq98hPqEX7fNZaFWoO5E.P6l/PgZc6cIQUlWnF04XUQx4bP4a"

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
