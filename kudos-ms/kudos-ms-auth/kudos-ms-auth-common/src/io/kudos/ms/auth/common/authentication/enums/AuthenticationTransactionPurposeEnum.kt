package io.kudos.ms.auth.common.authentication.enums

/** Security-sensitive intent carried by an authentication transaction. */
enum class AuthenticationTransactionPurposeEnum {
    LOGIN,
    STEP_UP,
    LINK_EXTERNAL_IDENTITY,
}
