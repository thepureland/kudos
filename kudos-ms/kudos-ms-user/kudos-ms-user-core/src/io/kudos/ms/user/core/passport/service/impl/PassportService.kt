package io.kudos.ms.user.core.passport.service.impl

import io.kudos.base.logger.LogFactory
import io.kudos.base.security.GoogleAuthenticator
import io.kudos.base.security.PasswordKit
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.account.dao.UserAccountDao
import io.kudos.ms.user.core.account.service.iservice.IUserAccountService
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
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
 *   4) BCrypt-verify the plaintext password.
 *      - On failure -> incrementLoginErrorTimes, return WRONG_PASSWORD + current error count.
 *   5) If authentication_key is not empty (OTP enabled):
 *      - Request does not carry authCode -> return OTP_REQUIRED (does not touch the error count).
 *      - authCode fails GoogleAuthenticator verification -> incrementLoginErrorTimes, return OTP_WRONG.
 *   6) All passes -> resetLoginErrorTimes + updateLastLoginInfo (if IP is present), return SUCCESS + UserInfoModel.
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
            log.debug(
                "Login failed - account frozen: userId=${user.id} type=${user.freezeType} " +
                    "window=[${user.freezeStartTime}, ${user.freezeEndTime})"
            )
            return PassportLoginResult.accountFrozen(user.freezeTitle)
        }

        val passwordMatches = PasswordKit.matches(req.plainPassword, user.loginPassword)
        if (!passwordMatches) {
            userAccountService.incrementLoginErrorTimes(user.id)
            // Use the Row queried directly via DAO to get the actual count after the increment;
            // do not rely on the cache (the cache may lag due to the event not being committed).
            val accumulated = userAccountService.getUserRecord(user.id)?.loginErrorTimes ?: ((user.loginErrorTimes ?: 0) + 1)
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
                userAccountService.incrementLoginErrorTimes(user.id)
                val accumulated = userAccountService.getUserRecord(user.id)?.loginErrorTimes
                    ?: ((user.loginErrorTimes ?: 0) + 1)
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
