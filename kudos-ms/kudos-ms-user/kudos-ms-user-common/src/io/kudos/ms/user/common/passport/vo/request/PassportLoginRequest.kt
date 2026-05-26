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

    /** Caller IP (stored as BigInt form); may be null to skip logging */
    val loginIp: Long? = null,

    /**
     * OTP verification code (6 digits).
     *
     * - When the user **has not enabled OTP** (i.e., `authentication_key` is empty): this field is ignored
     * - When the user **has enabled OTP**:
     *     - Not provided -> backend returns OTP_REQUIRED, frontend prompts for OTP input
     *     - Wrong value -> returns OTP_WRONG, equivalent to wrong password (error count already incremented)
     *     - Correct value -> equivalent to correct password, login succeeds
     */
    val authCode: Long? = null,

)
