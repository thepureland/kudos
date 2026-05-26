package io.kudos.ms.user.common.passport.vo.request

import jakarta.validation.constraints.NotBlank


/**
 * User self-service password change (login password or security password).
 *
 * Differs from administrator resetPassword: requires verification of the old password first.
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
