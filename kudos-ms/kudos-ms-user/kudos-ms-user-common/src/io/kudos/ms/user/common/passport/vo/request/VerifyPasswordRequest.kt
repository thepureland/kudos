package io.kudos.ms.user.common.passport.vo.request

import jakarta.validation.constraints.NotBlank


/**
 * Verify the user's current password (does not consume error attempts).
 *
 * Used for scenarios such as "re-confirming identity before sensitive operations". Differences from [PassportLoginRequest]:
 * - userId is known (no username/tenant lookup), avoiding re-lookup
 * - Does not update last_login_*, does not change loginErrorTimes
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
