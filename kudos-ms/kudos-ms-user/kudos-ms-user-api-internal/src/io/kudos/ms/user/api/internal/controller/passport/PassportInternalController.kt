package io.kudos.ms.user.api.internal.controller.passport

import io.kudos.ms.user.common.passport.api.IPassportApi
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.api.PassportApi
import org.springframework.web.bind.annotation.RestController


/**
 * 登录通行证 内部 RPC 控制器。路径继承自 [IPassportApi] 方法级注解。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
class PassportInternalController(
    private val passportApi: PassportApi,
) : IPassportApi {

    override fun login(req: PassportLoginRequest): PassportLoginResult = passportApi.login(req)

    override fun logout(userId: String): Boolean = passportApi.logout(userId)

    override fun verifyPassword(req: VerifyPasswordRequest): Boolean = passportApi.verifyPassword(req)

    override fun verifySecurityPassword(req: VerifyPasswordRequest): Boolean =
        passportApi.verifySecurityPassword(req)

    override fun changePassword(req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportApi.changePassword(req)

    override fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportApi.changeSecurityPassword(req)

}
