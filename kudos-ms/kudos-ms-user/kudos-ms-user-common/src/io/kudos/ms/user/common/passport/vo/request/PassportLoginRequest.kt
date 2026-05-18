package io.kudos.ms.user.common.passport.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank


/**
 * 登录请求
 *
 * @author K
 * @since 1.0.0
 */
data class PassportLoginRequest(

    /** 租户id */
    @get:NotBlank
    val tenantId: String,

    /** 用户名 */
    @get:NotBlank
    @get:MaxLength(64)
    val username: String,

    /** 明文密码。调用方自行决定传输安全（HTTPS / 端到端加密等） */
    @get:NotBlank
    val plainPassword: String,

    /** 登录方 IP（已转 BigInt 形式存储）；可为 null 不写日志 */
    val loginIp: Long? = null,

    /**
     * OTP 验证码（6 位数字）。
     *
     * - 用户**未启用 OTP**时（即 `authentication_key` 为空）：本字段被忽略
     * - 用户**已启用 OTP**时：
     *     - 不传 → 后端返回 OTP_REQUIRED，前端再弹 OTP 输入框
     *     - 传错 → 返回 OTP_WRONG，等同于密码错（错误计数已累加）
     *     - 传对 → 与密码正确同等，登录成功
     */
    val authCode: Long? = null,

)
