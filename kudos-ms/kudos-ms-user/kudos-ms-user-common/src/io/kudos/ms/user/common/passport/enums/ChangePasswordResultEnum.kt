package io.kudos.ms.user.common.passport.enums

/**
 * Result of changing password (login password / security password).
 *
 * Like [PassportLoginStatusEnum], HTTP layer always returns 200, with this enum distinguishing the reason.
 *
 * @author K
 * @since 1.0.0
 */
enum class ChangePasswordResultEnum {

    /** Change succeeded */
    SUCCESS,

    /** User not found */
    USER_NOT_FOUND,

    /** Old password is incorrect */
    OLD_PASSWORD_WRONG,
}
