package io.kudos.ms.user.common.passport.api

import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestParam


/**
 * 登录通行证 对外API
 *
 * 实现负责：用户查询、密码（BCrypt）校验、登录错误计数维护、最后登录信息写库。
 *
 * @author K
 * @since 1.0.0
 */
interface IPassportApi {

    /**
     * 校验凭据并完成登录。无论成功与否都返回 [PassportLoginResult]——HTTP 层始终 200。
     */
    @PostMapping("/api/internal/user/passport/login")
    fun login(@RequestBody req: PassportLoginRequest): PassportLoginResult

    /**
     * 登出：写入最后登出时间。仅负责落库审计，不做会话/JWT 撤销。
     */
    @PostMapping("/api/internal/user/passport/logout")
    fun logout(@RequestParam userId: String): Boolean

    /**
     * 校验当前用户的登录密码（不消耗错误次数，不更新登录时间）。
     *
     * 用于敏感操作前的二次身份确认（"are you really you?"）。
     *
     * @return true 匹配；false 用户不存在 / 密码错
     */
    @PostMapping("/api/internal/user/passport/verifyPassword")
    fun verifyPassword(@RequestBody req: VerifyPasswordRequest): Boolean

    /**
     * 校验当前用户的安全密码（不消耗错误次数）。
     */
    @PostMapping("/api/internal/user/passport/verifySecurityPassword")
    fun verifySecurityPassword(@RequestBody req: VerifyPasswordRequest): Boolean

    /**
     * 用户本人修改登录密码：先校验旧密码，正确才覆盖新密码。
     */
    @PostMapping("/api/internal/user/passport/changePassword")
    fun changePassword(@RequestBody req: ChangePasswordRequest): ChangePasswordResultEnum

    /**
     * 用户本人修改安全密码。
     */
    @PostMapping("/api/internal/user/passport/changeSecurityPassword")
    fun changeSecurityPassword(@RequestBody req: ChangePasswordRequest): ChangePasswordResultEnum

}
