package io.kudos.ms.user.common.passport.vo.response

import io.kudos.ms.user.common.passport.enums.PassportLoginStatusEnum
import java.io.Serializable


/**
 * Login result. All outcomes (success / user not found / wrong password / inactive) are returned
 * via this class; the HTTP layer always returns 200, with [status] distinguishing the result.
 *
 * @author K
 * @since 1.0.0
 */
data class PassportLoginResult(

    /** Login result status */
    val status: PassportLoginStatusEnum,

    /** User info, populated only when [status]=SUCCESS */
    val userInfo: UserInfoModel? = null,

    /** Cumulative login error count (including the current failure), populated only when [status]=WRONG_PASSWORD */
    val loginErrorTimes: Int? = null,

    /** Additional error description, may be null */
    val message: String? = null,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L

        fun success(userInfo: UserInfoModel): PassportLoginResult =
            PassportLoginResult(status = PassportLoginStatusEnum.SUCCESS, userInfo = userInfo)

        fun userNotFound(): PassportLoginResult =
            PassportLoginResult(status = PassportLoginStatusEnum.USER_NOT_FOUND, message = "User does not exist")

        fun wrongPassword(loginErrorTimes: Int): PassportLoginResult =
            PassportLoginResult(
                status = PassportLoginStatusEnum.WRONG_PASSWORD,
                loginErrorTimes = loginErrorTimes,
                message = "Incorrect password"
            )

        fun inactive(): PassportLoginResult =
            PassportLoginResult(status = PassportLoginStatusEnum.INACTIVE, message = "Account is disabled")

        /**
         * Account locked: consecutive login failures reached the configured threshold.
         * Further attempts are rejected until the lock window expires or an administrator intervenes.
         *
         * @param loginErrorTimes cumulative error count when known, may be null when answered from the freeze gate
         */
        fun locked(loginErrorTimes: Int? = null): PassportLoginResult =
            PassportLoginResult(
                status = PassportLoginStatusEnum.LOCKED,
                loginErrorTimes = loginErrorTimes,
                message = "Account is locked due to too many failed login attempts",
            )

        /** The user has enabled OTP but did not provide authCode; the client should prompt for the OTP and retry. */
        fun otpRequired(): PassportLoginResult =
            PassportLoginResult(status = PassportLoginStatusEnum.OTP_REQUIRED, message = "Please enter the dynamic verification code")

        /** OTP code is incorrect; the server has incremented login_error_times. */
        fun otpWrong(loginErrorTimes: Int): PassportLoginResult =
            PassportLoginResult(
                status = PassportLoginStatusEnum.OTP_WRONG,
                loginErrorTimes = loginErrorTimes,
                message = "Incorrect dynamic verification code",
            )

        /**
         * Account is frozen.
         *
         * @param freezeTitle from user_account.freeze_title, passed through to the frontend; default text is used when blank
         */
        fun accountFrozen(freezeTitle: String?): PassportLoginResult =
            PassportLoginResult(
                status = PassportLoginStatusEnum.ACCOUNT_FROZEN,
                message = freezeTitle?.takeIf { it.isNotBlank() } ?: "Account is frozen",
            )
    }
}
