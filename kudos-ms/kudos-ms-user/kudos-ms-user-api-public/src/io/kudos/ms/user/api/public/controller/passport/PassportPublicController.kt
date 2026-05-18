package io.kudos.ms.user.api.public.controller.passport

import io.kudos.base.security.BarcodeKit
import io.kudos.context.core.KudosContext
import io.kudos.ms.user.common.passport.CurrentUserKit
import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import io.kudos.ms.user.common.passport.vo.SessionUserPrincipal
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import io.kudos.ms.user.common.passport.vo.response.UserInfoModel
import io.kudos.ms.user.core.passport.service.iservice.IPassportService
import jakarta.servlet.http.HttpServletRequest
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
 * **会话模型**：登录成功时 `SessionUserPrincipal` 被写入 `HttpSession[SESSION_KEY_USER]`，
 * `UserContextWebFilter` 在后续请求里把它灌进 `KudosContext.user`。`CurrentUserKit` 是
 * 在控制器/服务里取当前用户的标准入口。
 *
 * **`@RequestParam userId` 的接口仍保留**（verifyPassword / changePassword / logout / 等）：
 * 既适配跨服务 RPC 调用（没有 session 上下文），也作为没启会话方案时的兜底；网关层应该保证
 * 这些接口在公开侧带 userId 时不能跨用户操作。
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
    fun login(@RequestBody @Valid req: PassportLoginRequest, request: HttpServletRequest): PassportLoginResult {
        val res = passportService.login(req)
        if (res.status == PassportLoginStatusEnum.SUCCESS) {
            val info = res.userInfo ?: return res
            // 写入 HttpSession，后续请求由 UserContextWebFilter 读出并灌入 KudosContext.user
            val principal = SessionUserPrincipal(
                id = info.id,
                tenantId = info.tenantId,
                username = info.username,
            )
            request.session.setAttribute(KudosContext.SESSION_KEY_USER, principal)
        }
        return res
    }

    /**
     * 登出：写最后登出时间作审计；同时 invalidate session 让 [UserContextWebFilter] 下次请求拿不到用户。
     *
     * 如果 [userId] 没传，则尝试从当前会话取——这样前端可以"我登出我自己"不需要再传 id。
     */
    @PostMapping("/logout")
    fun logout(
        @RequestParam(required = false) userId: String?,
        request: HttpServletRequest,
    ): Boolean {
        val effectiveUserId = userId ?: CurrentUserKit.currentUserIdOrNull() ?: return false
        val ok = passportService.logout(effectiveUserId)
        // session 不管 service 调用成败都干掉——既然客户端要登出
        request.getSession(false)?.invalidate()
        return ok
    }

    /**
     * 返回当前登录用户的简要信息；未登录返回 null。前端常用于 "刷新页面后我还登录着吗?" 的判断。
     */
    @GetMapping("/me")
    fun me(): UserInfoModel? {
        val p = CurrentUserKit.currentPrincipalOrNull() ?: return null
        // 这里只返回 session 里有的 3 个字段；要完整 profile 用 sysUserApi 再拉一次
        return UserInfoModel(
            id = p.id,
            username = p.username,
            tenantId = p.tenantId,
            orgId = null,
            accountTypeDictCode = null,
            defaultLocale = null,
            defaultTimezone = null,
            defaultCurrency = null,
            loginTime = java.time.LocalDateTime.now(),
        )
    }

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
     */
    @GetMapping("/qrCode", produces = [MediaType.IMAGE_PNG_VALUE])
    fun qrCode(
        @RequestParam text: String,
        @RequestParam(required = false, defaultValue = "200") size: Int,
    ): ByteArray = BarcodeKit.qrcodePng(text, size = size)

}
