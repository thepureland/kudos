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
 * 登录通行证业务实现
 *
 * 流程：
 *   1) 按 (tenantId, username) 取用户缓存项
 *   2) 不存在 → USER_NOT_FOUND
 *   3) active != true → INACTIVE
 *   4) BCrypt 校验明文密码
 *      - 失败 → incrementLoginErrorTimes，返回 WRONG_PASSWORD + 当前错误次数
 *   5) 若 authentication_key 非空（已启用 OTP）：
 *      - 请求未带 authCode → 返回 OTP_REQUIRED（不动错误计数）
 *      - authCode 不通过 GoogleAuthenticator 校验 → incrementLoginErrorTimes，返回 OTP_WRONG
 *   6) 全部通过 → resetLoginErrorTimes + updateLastLoginInfo（若有 IP），返回 SUCCESS + UserInfoModel
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
                log.debug("登录失败 - 用户不存在: tenantId=${req.tenantId} username=${req.username}")
                return PassportLoginResult.userNotFound()
            }

        if (user.active != true) {
            log.debug("登录失败 - 账号未启用: userId=${user.id}")
            return PassportLoginResult.inactive()
        }

        val passwordMatches = PasswordKit.matches(req.plainPassword, user.loginPassword)
        if (!passwordMatches) {
            userAccountService.incrementLoginErrorTimes(user.id)
            // 用 DAO 直查的 Row 拿到刚自增后的真实计数；不依赖缓存（缓存可能因事件未提交而滞后）
            val accumulated = userAccountService.getUserRecord(user.id)?.loginErrorTimes ?: ((user.loginErrorTimes ?: 0) + 1)
            log.debug("登录失败 - 密码错误: userId=${user.id} 累计错误次数=${accumulated}")
            return PassportLoginResult.wrongPassword(accumulated)
        }

        // 密码正确，检查是否启用 OTP 二次验证
        val authKey = user.authenticationKey
        if (!authKey.isNullOrBlank()) {
            val authCode = req.authCode
            if (authCode == null) {
                log.debug("登录待定 - 需要 OTP: userId=${user.id}")
                return PassportLoginResult.otpRequired()
            }
            val otpOk = GoogleAuthenticator().checkCode(authKey, authCode, System.currentTimeMillis())
            if (!otpOk) {
                userAccountService.incrementLoginErrorTimes(user.id)
                val accumulated = userAccountService.getUserRecord(user.id)?.loginErrorTimes
                    ?: ((user.loginErrorTimes ?: 0) + 1)
                log.debug("登录失败 - OTP 错误: userId=${user.id} 累计错误次数=${accumulated}")
                return PassportLoginResult.otpWrong(accumulated)
            }
        }

        // 验证全部通过：清零错误计数 + 记录最后登录信息
        userAccountService.resetLoginErrorTimes(user.id)
        val now = LocalDateTime.now()
        val loginIp = req.loginIp
        if (loginIp != null) {
            userAccountService.updateLastLoginInfo(user.id, loginIp, now)
        }

        log.debug("登录成功: userId=${user.id} username=${user.username}")
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
        if (success) log.debug("登出成功: userId=${userId}")
        else log.debug("登出失败（用户不存在？）: userId=${userId}")
        return success
    }

    @Transactional(readOnly = true)
    override fun verifyPassword(req: VerifyPasswordRequest): Boolean {
        // 直查 DAO：拿到最新的 loginPassword 哈希（不走缓存，避免落后于 changePassword 写操作）
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
        // 旧密码正确——直接调既有的 resetPassword（它会 hash 新密码并清错误计数）
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
}
