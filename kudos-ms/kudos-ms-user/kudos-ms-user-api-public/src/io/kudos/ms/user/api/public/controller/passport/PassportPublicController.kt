package io.kudos.ms.user.api.public.controller.passport

import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import jakarta.validation.Valid
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController


/**
 * 登录通行证 公开 HTTP 控制器（终端用户访问）。
 *
 * 所有结局（成功 / 用户不存在 / 密码错 / 禁用）一律 HTTP 200，由 [PassportLoginResult.status] 区分；
 * 前端按状态做差异化提示。
 *
 * @author K
 * @since 1.0.0
 */
@RestController
@RequestMapping("/api/public/user/passport")
class PassportPublicController(
    private val passportService: IPassportService,
) {

    @PostMapping("/login")
    fun login(@RequestBody @Valid req: PassportLoginRequest): PassportLoginResult =
        passportService.login(req)

}
