package io.kudos.ms.auth.core.authentication.lifecycle.service.iservice

import io.kudos.ms.auth.core.authentication.lifecycle.model.AuthenticationInvalidationResult

/** Coordinates token epoch, refresh-token families and logical sessions for account lifecycle changes. */
interface IAuthenticationLifecycleService {
    fun invalidateAll(tenantId: String, userId: String, reason: String): AuthenticationInvalidationResult
}
