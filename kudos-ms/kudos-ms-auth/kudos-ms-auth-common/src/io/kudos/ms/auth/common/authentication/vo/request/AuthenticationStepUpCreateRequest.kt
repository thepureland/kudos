package io.kudos.ms.auth.common.authentication.vo.request

import io.kudos.base.bean.validation.constraint.annotations.MaxLength

/** Starts fresh re-authentication for the current logical session. */
data class AuthenticationStepUpCreateRequest(
    @get:MaxLength(128)
    val requiredAcr: String,
    @get:MaxLength(32)
    val requestedMethod: String = "password",
)
