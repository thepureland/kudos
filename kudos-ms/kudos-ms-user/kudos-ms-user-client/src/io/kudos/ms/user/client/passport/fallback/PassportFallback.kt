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
 * 登录通行证 Feign 容错降级实现。
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
            message = "登录服务不可达，请稍后重试",
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
        // 用 USER_NOT_FOUND 作为"上游不可用"的占位——比 OLD_PASSWORD_WRONG 误导小一些。
        return ChangePasswordResultEnum.USER_NOT_FOUND
    }

    override fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum {
        errorWrite("changeSecurityPassword", req.userId)
        return ChangePasswordResultEnum.USER_NOT_FOUND
    }
}
