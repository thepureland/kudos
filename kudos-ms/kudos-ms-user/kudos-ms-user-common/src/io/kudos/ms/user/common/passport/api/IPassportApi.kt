package io.kudos.ms.user.common.passport.api

import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody


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
     *
     * @param req 登录请求（租户/用户名/明文密码/可选登录IP）
     * @return 登录结果（状态 + 可选用户信息 / 错误次数 / 描述）
     */
    @PostMapping("/api/internal/user/passport/login")
    fun login(@RequestBody req: PassportLoginRequest): PassportLoginResult

}
