package io.kudos.ms.user.common.passport.vo.request

import jakarta.validation.constraints.NotBlank


/**
 * 校验用户当前密码（不消耗错误次数）。
 *
 * 用于"敏感操作前再次确认身份"之类的场景，与 [PassportLoginRequest] 的差别：
 * - 已知 userId（不查 username/tenant），免去重新查找
 * - 不更新 last_login_*，不动 loginErrorTimes
 *
 * @author K
 * @since 1.0.0
 */
data class VerifyPasswordRequest(

    @get:NotBlank
    val userId: String,

    @get:NotBlank
    val plainPassword: String,

)
