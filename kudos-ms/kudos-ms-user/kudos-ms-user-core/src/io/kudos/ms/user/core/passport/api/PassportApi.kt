package io.kudos.ms.user.core.passport.api

import io.kudos.ms.user.common.passport.api.IPassportApi
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import org.springframework.context.annotation.Primary
import org.springframework.stereotype.Component


/**
 * Login passport API local implementation.
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

    override fun logout(userId: String): Boolean = passportService.logout(userId)

    override fun verifyPassword(req: VerifyPasswordRequest): Boolean =
        passportService.verifyPassword(req)

    override fun verifySecurityPassword(req: VerifyPasswordRequest): Boolean =
        passportService.verifySecurityPassword(req)

    override fun changePassword(req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportService.changePassword(req)

    override fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportService.changeSecurityPassword(req)
}
