package io.kudos.ms.auth.core.authentication.loginevent

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.AuthenticationMethodRegistry
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEvent
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventRecordCommand
import io.kudos.ms.auth.core.authentication.loginevent.service.iservice.IAuthLoginEventService
import io.kudos.ms.auth.core.authentication.service.impl.AuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.spi.AuthenticationChallenge
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodResult
import io.kudos.ms.auth.core.authentication.spi.IAuthenticationMethodProvider
import io.kudos.ms.auth.core.authentication.store.InMemoryAuthenticationTransactionStore
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * The feed contract: every authentication transaction that reaches a decision produces exactly one audit row,
 * and nothing that has not reached one produces any.
 */
internal class AuthLoginEventFeedTest {

    private val recorder = RecordingLoginEventService()
    private val store = InMemoryAuthenticationTransactionStore()
    private val service = AuthenticationTransactionService(
        store,
        AuthenticationMethodRegistry(listOf(StubProvider)),
        loginEventService = recorder,
    )

    @Test
    fun aCompletedLoginIsRecordedOnceWithItsSubjectMethodAndClientFacts() {
        val transaction = service.create(AuthenticationTransactionCreateRequest("t-1", "password"))

        service.act(transaction.id, AuthenticationActionEnum.VERIFY_PASSWORD, request("correct"))

        val event = recorder.commands.single()
        assertTrue(event.success)
        assertEquals(transaction.id, event.transactionId)
        assertEquals(AuthenticationTransactionPurposeEnum.LOGIN, event.purpose)
        assertEquals("t-1", event.tenantId)
        assertEquals("u-1", event.userId)
        assertEquals("alice", event.identifier)
        assertEquals("password", event.authenticationMethod)
        assertNull(event.failureCode)
        assertEquals(setOf("test"), event.amr)
        // Client facts come from what the public edge observed for this call, not from the payload's fields.
        assertEquals(3232235777L, event.observation.loginIp)
        assertEquals("Firefox", event.observation.loginBrowser)
    }

    @Test
    fun aTerminalFailureIsRecordedWithItsCodeAndTheAttemptedName() {
        val transaction = service.create(AuthenticationTransactionCreateRequest("t-1", "password"))

        service.act(transaction.id, AuthenticationActionEnum.VERIFY_PASSWORD, request("locked-out"))

        val event = recorder.commands.single()
        assertEquals(false, event.success)
        assertEquals("ACCOUNT_LOCKED", event.failureCode)
        // The name is carried even though no account matched it; the service digests it before storing.
        assertEquals("alice", event.identifier)
        assertNull(event.userId)
    }

    @Test
    fun aChallengeIsNotAnOutcomeAndIsNotRecordedUntilTheTransactionEnds() {
        val transaction = service.create(AuthenticationTransactionCreateRequest("t-1", "password"))

        service.act(transaction.id, AuthenticationActionEnum.VERIFY_PASSWORD, request("retryable"))

        assertEquals(emptyList(), recorder.commands)

        service.act(transaction.id, AuthenticationActionEnum.VERIFY_PASSWORD, request("correct"))

        assertEquals(1, recorder.commands.size)
    }

    @Test
    fun abandonedTransactionsAreNotLoginOutcomes() {
        val cancelled = service.create(AuthenticationTransactionCreateRequest("t-1", "password"))
        service.cancel(cancelled.id)

        val expired = service.create(AuthenticationTransactionCreateRequest("t-1", "password"))
        store.save(
            expiredCopy(requireNotNull(store.get(expired.id))),
            requireNotNull(store.get(expired.id)).version,
        )
        service.get(expired.id)

        // Somebody closing the tab is not a decision about their identity; recording it would bury the rows
        // that are.
        assertEquals(emptyList(), recorder.commands)
    }

    @Test
    fun aStepUpOutcomeIsRecordedUnderItsOwnPurpose() {
        val transaction = service.create(AuthenticationTransactionCreateRequest("t-1", "password"))
        service.act(transaction.id, AuthenticationActionEnum.VERIFY_PASSWORD, request("correct"))
        recorder.commands.clear()

        val stepUp = service.createStepUp(
            io.kudos.ms.auth.core.authentication.model.AuthenticationStepUpCreateCommand(
                userId = "u-1",
                tenantId = "t-1",
                sourceSessionId = "session-1",
                requiredAcr = "urn:test:acr",
                requestedMethod = "password",
                username = "alice",
            )
        )
        service.act(stepUp.id, AuthenticationActionEnum.VERIFY_PASSWORD, request("correct"))

        assertEquals(AuthenticationTransactionPurposeEnum.STEP_UP, recorder.commands.single().purpose)
    }

    private fun expiredCopy(transaction: AuthenticationTransaction) =
        transaction.copy(expiresAt = transaction.createdAt.minusSeconds(1))

    private fun request(password: String) = AuthenticationActionRequest(
        tenantId = "t-1",
        method = "password",
        username = "alice",
        plainPassword = password,
        loginIp = 3232235777L,
        loginBrowser = "Firefox",
        loginOs = "Linux",
        userAgent = "Mozilla/5.0",
    )

    private object StubProvider : IAuthenticationMethodProvider {
        override fun method(): String = "password"

        override fun begin(
            transaction: AuthenticationTransaction,
            request: AuthenticationTransactionCreateRequest,
        ) = AuthenticationChallenge(
            AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            AuthenticationActionEnum.VERIFY_PASSWORD,
        )

        override fun verify(
            transaction: AuthenticationTransaction,
            action: AuthenticationActionEnum,
            request: AuthenticationActionRequest,
        ) = when (request.plainPassword) {
            "correct" -> AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.SUCCESS,
                userId = "u-1",
                tenantId = transaction.tenantId,
                username = request.username,
                amr = setOf("test"),
                acr = "urn:test:acr",
            )

            "locked-out" -> AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.FAILURE,
                terminal = true,
                username = request.username,
                errorCode = "ACCOUNT_LOCKED",
            )

            else -> AuthenticationMethodResult(
                outcome = AuthenticationMethodOutcomeEnum.FAILURE,
                nextAction = AuthenticationActionEnum.VERIFY_PASSWORD,
                username = request.username,
                errorCode = "INVALID_CREDENTIALS",
            )
        }
    }

    private class RecordingLoginEventService : IAuthLoginEventService {
        val commands = mutableListOf<AuthLoginEventRecordCommand>()

        override fun record(command: AuthLoginEventRecordCommand): Boolean {
            commands += command
            return true
        }

        override fun identifierHash(identifier: String) = identifier

        override fun listRecent(
            tenantId: String,
            userId: String?,
            identifier: String?,
            successOnly: Boolean?,
            limit: Int,
        ): List<AuthLoginEvent> = emptyList()
    }
}
