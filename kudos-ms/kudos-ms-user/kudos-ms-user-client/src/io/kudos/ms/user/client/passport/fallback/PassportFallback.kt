package io.kudos.ms.user.client.passport.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.passport.proxy.IPassportProxy
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import org.springframework.stereotype.Component


/**
 * Login passport Feign fallback.
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class PassportFallback : AbstractFeignFallbackSupport("PassportFallback"), IPassportProxy {

    override fun login(req: PassportLoginRequest): PassportLoginResult {
        errorWrite("login", req.tenantId, req.username)
        return PassportLoginResult(
            status = PassportLoginStatusEnum.LOCKED,
            message = "Login service is unreachable, please retry later",
        )
    }

    override fun logout(userId: String): Boolean {
        errorWrite("logout", userId)
        return false
    }

    override fun verifyPassword(req: VerifyPasswordRequest): Boolean {
        errorWrite("verifyPassword", req.userId)
        return false
    }

    override fun verifySecurityPassword(req: VerifyPasswordRequest): Boolean {
        errorWrite("verifySecurityPassword", req.userId)
        return false
    }

    override fun changePassword(req: ChangePasswordRequest): ChangePasswordResultEnum {
        errorWrite("changePassword", req.userId)
        // Use USER_NOT_FOUND as a placeholder for "upstream unavailable" — less misleading than OLD_PASSWORD_WRONG.
        return ChangePasswordResultEnum.USER_NOT_FOUND
    }

    override fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum {
        errorWrite("changeSecurityPassword", req.userId)
        return ChangePasswordResultEnum.USER_NOT_FOUND
    }
}
