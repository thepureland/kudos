package io.kudos.ms.user.common.account.vo.response

import java.io.Serializable


/**
 * Return value for user OTP/TOTP enrollment.
 *
 * After the server generates a Base32 secret and persists it ([io.kudos.ms.user.core.account.model.po.UserAccount.authenticationKey]),
 * it also returns the `otpauth://` URL to the client for QR-code rendering (frontend can render locally with zxing/qrcode.js etc.).
 *
 * Flow:
 *   1) Administrator or the user themselves calls resetAuthKey -> backend generates the secret, persists it, and returns this DTO
 *   2) Client renders [otpauthUrl] as a QR code
 *   3) User scans with Google Authenticator etc., the app stores the secret
 *   4) User enters the 6-digit TOTP code, frontend calls verifyAuthCode to verify
 *
 * @author K
 * @since 1.0.0
 */
data class AuthKeySetup(

    /** Base32-encoded secret; matches the persisted content of [io.kudos.ms.user.core.account.model.po.UserAccount.authenticationKey] */
    val secret: String,

    /** `otpauth://totp/...?secret=...` URL, for the frontend to render the QR code */
    val otpauthUrl: String,

) : Serializable {

    companion object {
        private const val serialVersionUID = 1L
    }
}
