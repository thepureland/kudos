package io.kudos.ms.user.core.passport.api

import io.kudos.ms.user.common.passport.api.IPassportApi
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component


/**
 * 登录通行证 API本地实现
 *
 * @author K
 * @since 1.0.0
 */
@Primary
@Component
open class PassportApi(
    private val passportService: IPassportService,
) : IPassportApi {

    override fun login(req: PassportLoginRequest): PassportLoginResult = passportService.login(req)
}
