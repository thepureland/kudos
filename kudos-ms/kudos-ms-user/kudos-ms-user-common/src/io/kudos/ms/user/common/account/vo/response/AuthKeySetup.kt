package io.kudos.ms.user.common.account.vo.response

import java.io.Serializable


/**
 * 用户 OTP/TOTP 启用设置返回值。
 *
 * 服务端生成 Base32 secret 后落库（[io.kudos.ms.user.core.account.model.po.UserAccount.authenticationKey]），
 * 同时把 `otpauth://` URL 返回给客户端用于二维码渲染（前端可用 zxing/qrcode.js 等本地渲染）。
 *
 * 流程：
 *   1) 管理员或用户本人调用 resetAuthKey → 后端生成 secret，落库 + 返回本 DTO
 *   2) 客户端把 [otpauthUrl] 渲染为二维码
 *   3) 用户用 Google Authenticator 等扫描，App 端存下 secret
 *   4) 用户输入 6 位 TOTP 验证码，前端调 verifyAuthCode 校验
 *
 * @author K
 * @since 1.0.0
 */
data class AuthKeySetup(

    /** Base32 编码的 secret；与 [io.kudos.ms.user.core.account.model.po.UserAccount.authenticationKey] 落库内容一致 */
    val secret: String,

    /** `otpauth://totp/...?secret=...` URL，供前端渲染二维码 */
    val otpauthUrl: String,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
