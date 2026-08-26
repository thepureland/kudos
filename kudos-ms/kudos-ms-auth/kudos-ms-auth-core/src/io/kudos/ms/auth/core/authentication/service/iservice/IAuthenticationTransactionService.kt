package io.kudos.ms.auth.core.authentication.service.iservice

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.model.AuthenticationStepUpCreateCommand

interface IAuthenticationTransactionService {
    fun create(request: AuthenticationTransactionCreateRequest): AuthenticationTransaction
    fun createStepUp(command: AuthenticationStepUpCreateCommand): AuthenticationTransaction =
        throw UnsupportedOperationException("Step-up authentication is not supported")
    fun get(id: String): AuthenticationTransaction?
    fun act(id: String, action: AuthenticationActionEnum, request: AuthenticationActionRequest): AuthenticationTransaction
    /** Binds a completed transaction to a logical, non-bearer Kudos session id. */
    fun bindSession(id: String, sessionId: String): AuthenticationTransaction
    fun cancel(id: String): AuthenticationTransaction?

    fun prepareExternal(
        id: String,
        providerId: String,
        tenantId: String,
        externalInvitationId: String? = null,
    ): AuthenticationTransaction

    fun createExternalLink(userId: String, tenantId: String, providerId: String): AuthenticationTransaction

    fun completeExternal(
        id: String,
        providerId: String,
        userId: String,
        tenantId: String,
        username: String,
        providerCode: String,
    ): AuthenticationTransaction

    fun failExternal(id: String, providerId: String, errorCode: String): AuthenticationTransaction?

    fun completeExternalLink(id: String, providerId: String, userId: String): AuthenticationTransaction
}
