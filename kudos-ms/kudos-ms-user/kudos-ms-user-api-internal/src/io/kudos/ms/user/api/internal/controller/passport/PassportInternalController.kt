package io.kudos.ms.user.api.internal.controller.passport

import io.kudos.ms.user.common.passport.api.IPassportApi
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
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

}
