package io.kudos.ms.user.api.public.controller.passport

import io.kudos.base.security.BarcodeKit
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import jakarta.validation.Valid
import org.springframework.http.MediaType
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController


/**
 * 登录通行证 公开 HTTP 控制器（终端用户访问）。
 *
 * **库模式说明**：本模块本身不包含会话/认证基础设施。所有 my-account 自助接口
 * 显式接受 `userId`（在 body 里），消费方（网关 / Spring Security / Sa-Token 等）
 * 应该把"当前已认证用户的 id"放进请求里，并在网关层确保只有该用户能操作自己的数据。
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

    /**
     * 登出：写最后登出时间作审计。前端在调用本接口之外仍需自行清理 cookie / 本地 token。
     */
    @PostMapping("/logout")
    fun logout(@RequestParam userId: String): Boolean = passportService.logout(userId)

    /** 校验当前用户的登录密码（不消耗错误次数）。用于敏感操作前的二次身份确认。 */
    @PostMapping("/verifyPassword")
    fun verifyPassword(@RequestBody @Valid req: VerifyPasswordRequest): Boolean =
        passportService.verifyPassword(req)

    /** 校验当前用户的安全密码（不消耗错误次数）。 */
    @PostMapping("/verifySecurityPassword")
    fun verifySecurityPassword(@RequestBody @Valid req: VerifyPasswordRequest): Boolean =
        passportService.verifySecurityPassword(req)

    /** 用户本人修改登录密码：先校验旧密码。 */
    @PostMapping("/changePassword")
    fun changePassword(@RequestBody @Valid req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportService.changePassword(req)

    /** 用户本人修改安全密码。 */
    @PostMapping("/changeSecurityPassword")
    fun changeSecurityPassword(@RequestBody @Valid req: ChangePasswordRequest): ChangePasswordResultEnum =
        passportService.changeSecurityPassword(req)

    /**
     * 把任意短文本（典型如 `otpauth://...`）渲染为 PNG 二维码。
     *
     * 一般搭配 [io.kudos.ms.user.common.account.vo.response.AuthKeySetup.otpauthUrl] 使用：
     * 前端拿到 otpauth URL 后 GET 本接口、把响应当 `<img>` 显示即可。
     *
     * @param text 二维码承载文本（URL-encoded 由 Spring 自动解码）
     * @param size 像素边长，默认 200
     * @return PNG 字节流
     */
    @GetMapping("/qrCode", produces = [MediaType.IMAGE_PNG_VALUE])
    fun qrCode(
        @RequestParam text: String,
        @RequestParam(required = false, defaultValue = "200") size: Int,
    ): ByteArray = BarcodeKit.qrcodePng(text, size = size)

}
