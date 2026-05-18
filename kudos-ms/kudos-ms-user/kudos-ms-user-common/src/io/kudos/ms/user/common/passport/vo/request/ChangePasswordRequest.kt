package io.kudos.ms.user.common.passport.vo.request

import jakarta.validation.constraints.NotBlank


/**
 * 用户本人修改密码（登录密码或安全密码）。
 *
 * 与管理员的 resetPassword 区别：要求先校验旧密码。
 *
 * @author K
 * @since 1.0.0
 */
data class ChangePasswordRequest(

    @get:NotBlank
    val userId: String,

    @get:NotBlank
    val oldPlainPassword: String,

    @get:NotBlank
    val newPlainPassword: String,

)
