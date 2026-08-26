package io.kudos.ms.user.common.passport.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength
import jakarta.validation.constraints.NotBlank


/**
 * Login request
 *
 * @author K
 * @since 1.0.0
 */
data class PassportLoginRequest(

    /** Tenant id */
    @get:NotBlank
    val tenantId: String,

    /** Username */
    @get:NotBlank
    @get:MaxLength(64)
    val username: String,

    /** Plain-text password. The caller is responsible for transport security (HTTPS / end-to-end encryption, etc.) */
    @get:NotBlank
    val plainPassword: String,

    /**
     * Login IP (stored as BigInt form).
     *
     * Public HTTP endpoints overwrite this value with server-observed request metadata; it is
     * retained on the common request model for trusted internal callers.
     */
    val loginIp: Long? = null,

    /**
     * OTP verification code (6 digits).
     *
     * - When the user **has not enabled OTP** (i.e., `authentication_key` is empty): this field is ignored
     * - When the user **has enabled OTP**:
     *     - Not provided -> backend returns OTP_REQUIRED, frontend prompts for OTP input
     *     - Wrong value -> returns OTP_WRONG and consumes the independent TOTP failure window
     *     - Correct value -> equivalent to correct password, login succeeds
     */
    val authCode: Long? = null,

    /** Client terminal type observed by the server (PC / Mobile / App / unknown). */
    val loginDevice: String? = null,

    /** Browser name and version observed by the server. */
    val loginBrowser: String? = null,

    /** Operating system name and version observed by the server. */
    val loginOs: String? = null,

    /** Raw User-Agent observed by the server. */
    val userAgent: String? = null,

    /** Single-use MFA recovery code. Ignored unless TOTP is enabled and [authCode] is absent. */
    @get:MaxLength(32)
    val recoveryCode: String? = null,

)
