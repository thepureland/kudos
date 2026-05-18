package io.kudos.ms.user.client.passport.fallback

import io.kudos.ability.distributed.client.feign.fallback.AbstractFeignFallbackSupport
import io.kudos.ms.user.client.passport.proxy.IPassportProxy
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import org.springframework.stereotype.Component


/**
 * 登录通行证 Feign 容错降级实现。
 *
 * 降级返回一个非 SUCCESS 的 [PassportLoginResult]，调用方根据状态决定后续动作（重试 / 提示）。
 *
 * @author K
 * @since 1.0.0
 */
@Component
open class PassportFallback : AbstractFeignFallbackSupport("PassportFallback"), IPassportProxy {

    override fun login(req: PassportLoginRequest): PassportLoginResult {
        errorWrite("login", req.tenantId, req.username)
        // 用 LOCKED 作为"上游不可用"的占位状态——和真正的"账号被锁定"语义并不一致，
        // 但避免新增枚举位；调用方应同时检查 message 字段判定具体原因。
        return PassportLoginResult(
            status = PassportLoginStatusEnum.LOCKED,
            message = "登录服务不可达，请稍后重试",
        )
    }

    override fun logout(userId: String): Boolean {
        errorWrite("logout", userId)
        return false
    }
}
