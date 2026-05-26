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
 * Passport (login) external API
 *
 * The implementation is responsible for: user lookup, password (BCrypt) verification, login error count maintenance, and persisting last-login info.
 *
 * @author K
 * @since 1.0.0
 */
interface IPassportApi {

    /**
     * Verify credentials and complete login. Returns [PassportLoginResult] regardless of success — HTTP layer always returns 200.
     */
    @PostMapping("/api/internal/user/passport/login")
    fun login(@RequestBody req: PassportLoginRequest): PassportLoginResult

    /**
     * Logout: writes the last-logout time. Only responsible for audit persistence; does not perform session/JWT revocation.
     */
    @PostMapping("/api/internal/user/passport/logout")
    fun logout(@RequestParam userId: String): Boolean

    /**
     * Verify the current user's login password (does not consume error attempts, does not update login time).
     *
     * Used for re-confirming identity before sensitive operations ("are you really you?").
     *
     * @return true if matched; false if user not found / password incorrect
     */
    @PostMapping("/api/internal/user/passport/verifyPassword")
    fun verifyPassword(@RequestBody req: VerifyPasswordRequest): Boolean

    /**
     * Verify the current user's security password (does not consume error attempts).
     */
    @PostMapping("/api/internal/user/passport/verifySecurityPassword")
    fun verifySecurityPassword(@RequestBody req: VerifyPasswordRequest): Boolean

    /**
     * User self-service login password change: verifies the old password first, only overwrites with the new password if correct.
     */
    @PostMapping("/api/internal/user/passport/changePassword")
    fun changePassword(@RequestBody req: ChangePasswordRequest): ChangePasswordResultEnum

    /**
     * User self-service security password change.
     */
    @PostMapping("/api/internal/user/passport/changeSecurityPassword")
    fun changeSecurityPassword(@RequestBody req: ChangePasswordRequest): ChangePasswordResultEnum

}
