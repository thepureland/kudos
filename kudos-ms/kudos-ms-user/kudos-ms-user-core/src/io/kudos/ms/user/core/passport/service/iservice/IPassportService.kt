package io.kudos.ms.user.core.passport.service.iservice

import io.kudos.ms.user.common.passport.enums.ChangePasswordResultEnum
import io.kudos.ms.user.common.passport.vo.request.ChangePasswordRequest
import io.kudos.ms.user.common.passport.vo.request.PassportLoginRequest
import io.kudos.ms.user.common.passport.vo.request.VerifyPasswordRequest
import io.kudos.ms.user.common.passport.vo.response.PassportLoginResult


/**
 * Login passport business interface.
 *
 * @author K
 * @since 1.0.0
 */
interface IPassportService {

    /**
     * Verifies login credentials and handles login side effects (error count / last login info).
     */
    fun login(req: PassportLoginRequest): PassportLoginResult

    /**
     * Logout: writes the last logout time.
     *
     * Does not perform session / JWT revocation -- upper-layer session cleanup is performed by the caller outside this method.
     */
    fun logout(userId: String): Boolean

    /**
     * Verifies the current user's login password (does not consume the error count, does not update the login time).
     * Used for secondary identity confirmation before sensitive operations.
     */
    fun verifyPassword(req: VerifyPasswordRequest): Boolean

    /**
     * Verifies the current user's security password (does not consume the error count).
     */
    fun verifySecurityPassword(req: VerifyPasswordRequest): Boolean

    /**
     * The user changes the login password themselves: verify the old password first, only overwrite with the new password if correct.
     */
    fun changePassword(req: ChangePasswordRequest): ChangePasswordResultEnum

    /**
     * The user changes the security password themselves.
     */
    fun changeSecurityPassword(req: ChangePasswordRequest): ChangePasswordResultEnum

}
