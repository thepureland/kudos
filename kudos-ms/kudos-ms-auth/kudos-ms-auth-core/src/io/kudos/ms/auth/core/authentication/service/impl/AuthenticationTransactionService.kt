package io.kudos.ms.auth.core.authentication.service.impl

import io.kudos.ms.auth.common.authentication.enums.AuthenticationActionEnum
import io.kudos.ms.auth.common.authentication.mfa.policy.enums.MfaMethodEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionPurposeEnum
import io.kudos.ms.auth.common.authentication.enums.AuthenticationTransactionStatusEnum
import io.kudos.ms.auth.common.authentication.vo.AuthenticationContext
import io.kudos.ms.auth.common.authentication.vo.AuthenticationTransaction
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationActionRequest
import io.kudos.ms.auth.common.authentication.vo.request.AuthenticationTransactionCreateRequest
import io.kudos.ms.auth.core.authentication.AuthenticationMethodRegistry
import io.kudos.ms.auth.core.authentication.assurance.DefaultAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.assurance.spi.IAuthenticationAssurancePolicy
import io.kudos.ms.auth.core.authentication.model.AuthenticationStepUpCreateCommand
import io.kudos.ms.auth.core.authentication.mfa.policy.AuthenticationMfaPolicyEnforcer
import io.kudos.ms.auth.core.authentication.mfa.policy.MfaPolicyEnforcementOutcomeEnum
import io.kudos.ms.auth.core.authentication.mfa.FederatedSecondFactorVerifier
import io.kudos.ms.auth.core.authentication.mfa.AuthenticationSecondFactorRegistry
import io.kudos.ms.auth.core.authentication.mfa.SecondFactorVerificationResult
import io.kudos.ms.auth.core.authentication.service.iservice.IAuthenticationTransactionService
import io.kudos.ms.auth.core.authentication.spi.AuthenticationChallenge
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodOutcomeEnum
import io.kudos.ms.auth.core.authentication.spi.AuthenticationMethodResult
import io.kudos.ms.auth.core.authentication.store.IAuthenticationTransactionStore
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventObservation
import io.kudos.ms.auth.core.authentication.loginevent.model.AuthLoginEventRecordCommand
import io.kudos.ms.auth.core.authentication.loginevent.service.iservice.IAuthLoginEventService
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import java.util.UUID

/** Coordinates secret-free, resumable authentication transactions. */
@Service
open class AuthenticationTransactionService(
    private val store: IAuthenticationTransactionStore,
    private val methodRegistry: AuthenticationMethodRegistry,
    private val assurancePolicy: IAuthenticationAssurancePolicy = DefaultAuthenticationAssurancePolicy(),
    private val mfaPolicyEnforcer: AuthenticationMfaPolicyEnforcer? = null,
    private val federatedSecondFactorVerifier: FederatedSecondFactorVerifier? = null,
    private val secondFactorRegistry: AuthenticationSecondFactorRegistry = AuthenticationSecondFactorRegistry(emptyList()),
    private val loginEventService: IAuthLoginEventService? = null,
) : IAuthenticationTransactionService {

    @Value($$"${kudos.ms.auth.authentication.transaction-ttl-seconds:300}")
    protected var transactionTtlSeconds: Long = 300

    override fun create(request: AuthenticationTransactionCreateRequest): AuthenticationTransaction {
        val now = Instant.now()
        var transaction = AuthenticationTransaction(
            id = UUID.randomUUID().toString(),
            tenantId = request.tenantId?.trim()?.takeIf { it.isNotEmpty() },
            status = AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
            nextActions = setOf(AuthenticationActionEnum.SELECT_METHOD),
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plus(transactionTtlSeconds.coerceAtLeast(1), ChronoUnit.SECONDS),
        )
        request.requestedMethod?.takeIf { it.isNotBlank() }?.let { method ->
            transaction = beginMethod(transaction, method, request)
        }
        check(store.create(transaction)) { "Authentication transaction id collision" }
        return transaction
    }

    override fun createStepUp(command: AuthenticationStepUpCreateCommand): AuthenticationTransaction {
        require(command.userId.isNotBlank()) { "Step-up user id must not be blank" }
        require(command.tenantId.isNotBlank()) { "Step-up tenant id must not be blank" }
        require(command.sourceSessionId.isNotBlank()) { "Step-up source session id must not be blank" }
        require(command.requiredAcr.isNotBlank()) { "Step-up required ACR must not be blank" }
        require(command.requestedMethod.isNotBlank()) { "Step-up authentication method must not be blank" }
        val now = Instant.now()
        val base = AuthenticationTransaction(
            id = UUID.randomUUID().toString(),
            tenantId = command.tenantId.trim(),
            purpose = AuthenticationTransactionPurposeEnum.STEP_UP,
            initiatorUserId = command.userId.trim(),
            sourceSessionId = command.sourceSessionId.trim(),
            requiredAcr = command.requiredAcr.trim(),
            userId = command.userId.trim(),
            username = command.username?.trim()?.takeIf(String::isNotEmpty),
            status = AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
            nextActions = emptySet(),
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plus(transactionTtlSeconds.coerceAtLeast(1), ChronoUnit.SECONDS),
        )
        val transaction = beginMethod(
            base,
            command.requestedMethod,
            AuthenticationTransactionCreateRequest(base.tenantId, command.requestedMethod),
        )
        check(store.create(transaction)) { "Authentication transaction id collision" }
        return transaction
    }

    override fun get(id: String): AuthenticationTransaction? {
        val current = store.get(id) ?: return null
        if (current.isTerminal() || Instant.now().isBefore(current.expiresAt)) return current
        return save(
            current,
            current.copy(
                status = AuthenticationTransactionStatusEnum.EXPIRED,
                nextActions = emptySet(),
                updatedAt = Instant.now(),
                errorCode = "AUTHENTICATION_TRANSACTION_EXPIRED",
            )
        )
    }

    override fun act(
        id: String,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): AuthenticationTransaction {
        val current = requireNotNull(get(id)) { "Authentication transaction not found: $id" }
        check(!current.isTerminal()) { "Authentication transaction is terminal: ${current.status}" }
        check(action in current.nextActions) { "Action $action is not allowed; expected ${current.nextActions}" }

        val updated = when (action) {
            AuthenticationActionEnum.SELECT_METHOD -> {
                val method = requireNotNull(request.method?.takeIf { it.isNotBlank() }) {
                    "Authentication method is required"
                }
                beginMethod(current, method, AuthenticationTransactionCreateRequest(current.tenantId, method))
            }

            AuthenticationActionEnum.SELECT_TENANT -> {
                val tenantId = requireNotNull(request.tenantId?.trim()?.takeIf { it.isNotEmpty() }) {
                    "Tenant id is required"
                }
                val withTenant = current.copy(tenantId = tenantId)
                current.method?.let { method ->
                    beginMethod(withTenant, method, AuthenticationTransactionCreateRequest(tenantId, method))
                } ?: withTenant.copy(
                    status = AuthenticationTransactionStatusEnum.WAITING_FOR_ACTION,
                    nextActions = setOf(AuthenticationActionEnum.SELECT_METHOD),
                )
            }

            else -> {
                if (isPendingPluggableSecondFactor(current, action)) {
                    applyPluggableSecondFactor(current, action, request)
                } else if (isPendingFederatedMfa(current, action)) {
                    applyFederatedSecondFactor(current, action, request)
                } else {
                    val method = requireNotNull(current.method) { "Authentication method has not been selected" }
                    val provider = requireNotNull(methodRegistry.find(method)) {
                        "Unsupported authentication method: $method"
                    }
                    applyMethodResult(current, provider.verify(current, action, request))
                }
            }
        }
        // The observation is whatever the public edge collected server-side for this call; the request body
        // cannot supply it, and an external callback simply has none to give.
        return save(current, updated.copy(updatedAt = Instant.now()), request.toObservation())
    }

    override fun bindSession(id: String, sessionId: String): AuthenticationTransaction {
        require(sessionId.isNotBlank()) { "Session id must not be blank" }
        val current = requireNotNull(get(id)) { "Authentication transaction not found: $id" }
        check(current.status == AuthenticationTransactionStatusEnum.COMPLETED) {
            "Only a completed authentication transaction can be bound to a session"
        }
        val context = requireNotNull(current.context)
        if (context.sessionId == sessionId) return current
        return save(
            current,
            current.copy(context = context.copy(sessionId = sessionId), updatedAt = Instant.now())
        )
    }

    override fun cancel(id: String): AuthenticationTransaction? {
        val current = get(id) ?: return null
        if (current.isTerminal()) return current
        return save(
            current,
            current.copy(
                status = AuthenticationTransactionStatusEnum.CANCELLED,
                nextActions = emptySet(),
                updatedAt = Instant.now(),
            )
        )
    }

    override fun prepareExternal(
        id: String,
        providerId: String,
        tenantId: String,
        externalInvitationId: String?,
    ): AuthenticationTransaction {
        require(providerId.isNotBlank()) { "Provider id must not be blank" }
        require(tenantId.isNotBlank()) { "Tenant id must not be blank" }
        val current = requireNotNull(get(id)) { "Authentication transaction not found: $id" }
        check(!current.isTerminal()) { "Authentication transaction is terminal: ${current.status}" }
        check(current.tenantId == null || current.tenantId == tenantId) {
            "Authentication transaction tenant does not match provider tenant"
        }
        val method = externalMethod(providerId)
        if (current.method == method &&
            current.nextActions == setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER)
        ) {
            check(current.externalInvitationId == externalInvitationId) {
                "Authentication transaction invitation cannot be replaced"
            }
            return current
        }
        check(current.method == null && AuthenticationActionEnum.SELECT_METHOD in current.nextActions) {
            "Authentication transaction already selected method ${current.method}"
        }
        return save(
            current,
            current.copy(
                tenantId = tenantId,
                method = method,
                externalInvitationId = externalInvitationId,
                status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
                nextActions = setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER),
                updatedAt = Instant.now(),
                errorCode = null,
            )
        )
    }

    override fun createExternalLink(
        userId: String,
        tenantId: String,
        providerId: String,
    ): AuthenticationTransaction {
        require(userId.isNotBlank()) { "User id must not be blank" }
        require(tenantId.isNotBlank()) { "Tenant id must not be blank" }
        require(providerId.isNotBlank()) { "Provider id must not be blank" }
        val now = Instant.now()
        val transaction = AuthenticationTransaction(
            id = UUID.randomUUID().toString(),
            tenantId = tenantId,
            purpose = AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY,
            initiatorUserId = userId,
            status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            nextActions = setOf(AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER),
            method = externalMethod(providerId),
            createdAt = now,
            updatedAt = now,
            expiresAt = now.plus(transactionTtlSeconds.coerceAtLeast(1), ChronoUnit.SECONDS),
        )
        check(store.create(transaction)) { "Authentication transaction id collision" }
        return transaction
    }

    override fun completeExternal(
        id: String,
        providerId: String,
        userId: String,
        tenantId: String,
        username: String,
        providerCode: String,
    ): AuthenticationTransaction {
        val current = requireNotNull(get(id)) { "Authentication transaction not found: $id" }
        check(current.method == externalMethod(providerId)) { "External provider does not match transaction" }
        check(current.status == AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED &&
            AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER in current.nextActions
        ) { "Authentication transaction is not awaiting an external callback" }
        check(current.tenantId == tenantId) { "External identity tenant does not match transaction" }
        val now = Instant.now()
        val amr = setOf(METHOD_FEDERATED, providerCode.trim().lowercase())
        val enforcement = mfaPolicyEnforcer?.enforce(
            AuthenticationTransactionPurposeEnum.LOGIN,
            tenantId,
            userId,
            ACR_FEDERATED,
        )
        val secondFactorActions = if (enforcement?.outcome == MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR) {
            availableFederatedSecondFactors(
                userId,
                tenantId,
                enforcement.decision?.policy?.allowedMethods ?: setOf(MfaMethodEnum.TOTP),
            )
        } else {
            emptySet()
        }
        if (secondFactorActions.isNotEmpty()) {
            return save(
                current,
                current.copy(
                    userId = userId,
                    username = username,
                    status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
                    nextActions = secondFactorActions,
                    amr = amr,
                    acr = ACR_FEDERATED,
                    context = null,
                    updatedAt = now,
                    errorCode = "MFA_REQUIRED",
                )
            )
        }
        if (enforcement?.outcome == MfaPolicyEnforcementOutcomeEnum.DENY_ENROLLMENT_REQUIRED ||
            enforcement?.outcome == MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR
        ) {
            val errorCode = if (enforcement.outcome == MfaPolicyEnforcementOutcomeEnum.REQUIRE_SECOND_FACTOR) {
                "MFA_REQUIRED"
            } else {
                "MFA_ENROLLMENT_REQUIRED"
            }
            return save(
                current,
                current.copy(
                    userId = userId,
                    username = username,
                    status = AuthenticationTransactionStatusEnum.FAILED,
                    nextActions = emptySet(),
                    updatedAt = now,
                    errorCode = errorCode,
                )
            )
        }
        val postActions = if (
            enforcement?.outcome == MfaPolicyEnforcementOutcomeEnum.ALLOW_ENROLLMENT_REQUIRED
        ) {
            setOf(AuthenticationActionEnum.ENROLL_MFA)
        } else {
            emptySet()
        }
        return save(
            current,
            current.copy(
                userId = userId,
                username = username,
                status = AuthenticationTransactionStatusEnum.COMPLETED,
                nextActions = emptySet(),
                amr = amr,
                acr = ACR_FEDERATED,
                context = AuthenticationContext(
                    userId = userId,
                    tenantId = tenantId,
                    authTime = now,
                    amr = amr,
                    acr = ACR_FEDERATED,
                ),
                updatedAt = now,
                errorCode = null,
                postAuthenticationActions = postActions,
            )
        )
    }

    override fun failExternal(
        id: String,
        providerId: String,
        errorCode: String,
    ): AuthenticationTransaction? {
        val current = get(id) ?: return null
        if (current.isTerminal()) return current
        check(current.method == externalMethod(providerId)) { "External provider does not match transaction" }
        return save(
            current,
            current.copy(
                status = AuthenticationTransactionStatusEnum.FAILED,
                nextActions = emptySet(),
                updatedAt = Instant.now(),
                errorCode = errorCode.take(128),
            )
        )
    }

    override fun completeExternalLink(
        id: String,
        providerId: String,
        userId: String,
    ): AuthenticationTransaction {
        val current = requireNotNull(get(id)) { "Authentication transaction not found: $id" }
        check(current.purpose == AuthenticationTransactionPurposeEnum.LINK_EXTERNAL_IDENTITY) {
            "Authentication transaction is not an external identity link"
        }
        check(current.initiatorUserId == userId) { "External identity link initiator does not match" }
        check(current.method == externalMethod(providerId)) { "External provider does not match transaction" }
        check(current.status == AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED &&
            AuthenticationActionEnum.REDIRECT_EXTERNAL_PROVIDER in current.nextActions
        ) { "Authentication transaction is not awaiting an external callback" }
        return save(
            current,
            current.copy(
                userId = userId,
                status = AuthenticationTransactionStatusEnum.COMPLETED,
                nextActions = emptySet(),
                updatedAt = Instant.now(),
                errorCode = null,
            )
        )
    }

    private fun beginMethod(
        transaction: AuthenticationTransaction,
        method: String,
        request: AuthenticationTransactionCreateRequest,
    ): AuthenticationTransaction {
        val normalizedMethod = method.trim().lowercase()
        val provider = methodRegistry.find(normalizedMethod)
            ?: return transaction.copy(
                method = normalizedMethod,
                status = AuthenticationTransactionStatusEnum.FAILED,
                nextActions = emptySet(),
                errorCode = "UNSUPPORTED_AUTHENTICATION_METHOD",
            )
        if (!provider.supports(transaction)) {
            return transaction.copy(
                method = normalizedMethod,
                status = AuthenticationTransactionStatusEnum.FAILED,
                nextActions = emptySet(),
                errorCode = "AUTHENTICATION_METHOD_NOT_AVAILABLE",
            )
        }
        val challenge: AuthenticationChallenge = provider.begin(transaction, request)
        return transaction.copy(
            method = normalizedMethod,
            status = challenge.status,
            nextActions = setOf(challenge.nextAction),
            errorCode = null,
        )
    }

    private fun applyMethodResult(
        transaction: AuthenticationTransaction,
        result: AuthenticationMethodResult,
    ): AuthenticationTransaction = when (result.outcome) {
        AuthenticationMethodOutcomeEnum.SUCCESS -> {
            val userId = requireNotNull(result.userId) { "Successful authentication must provide userId" }
            val tenantId = requireNotNull(result.tenantId ?: transaction.tenantId) {
                "Successful authentication must provide tenantId"
            }
            val acr = requireNotNull(result.acr) { "Successful authentication must provide acr" }
            val now = Instant.now()
            val subjectMismatch = transaction.purpose == AuthenticationTransactionPurposeEnum.STEP_UP &&
                (transaction.initiatorUserId != userId || transaction.tenantId != tenantId)
            val requiredAcr = transaction.requiredAcr
            val insufficientAssurance = transaction.purpose == AuthenticationTransactionPurposeEnum.STEP_UP &&
                (requiredAcr == null || !assurancePolicy.isSatisfied(acr, requiredAcr))
            when {
                subjectMismatch -> transaction.copy(
                    status = AuthenticationTransactionStatusEnum.FAILED,
                    nextActions = emptySet(),
                    errorCode = "STEP_UP_SUBJECT_MISMATCH",
                )

                insufficientAssurance -> transaction.copy(
                    status = AuthenticationTransactionStatusEnum.FAILED,
                    nextActions = emptySet(),
                    errorCode = "REQUIRED_ACR_NOT_SATISFIED",
                )

                else -> transaction.copy(
                    tenantId = tenantId,
                    userId = userId,
                    username = result.username,
                    status = AuthenticationTransactionStatusEnum.COMPLETED,
                    nextActions = emptySet(),
                    amr = result.amr,
                    acr = acr,
                    context = AuthenticationContext(
                        userId = userId,
                        tenantId = tenantId,
                        authTime = now,
                        amr = result.amr,
                        acr = acr,
                    ),
                    errorCode = null,
                    postAuthenticationActions = result.postAuthenticationActions,
                )
            }
        }

        AuthenticationMethodOutcomeEnum.CHALLENGE -> transaction.copy(
            tenantId = result.tenantId ?: transaction.tenantId,
            userId = result.userId ?: transaction.userId,
            username = result.username ?: transaction.username,
            status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
            nextActions = result.nextActions.ifEmpty { setOf(requireNotNull(result.nextAction)) },
            amr = result.amr.ifEmpty { transaction.amr },
            acr = result.acr ?: transaction.acr,
            context = null,
            errorCode = result.errorCode,
        )

        AuthenticationMethodOutcomeEnum.FAILURE -> transaction.copy(
            username = result.username ?: transaction.username,
            status = if (result.terminal) {
                AuthenticationTransactionStatusEnum.FAILED
            } else {
                AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED
            },
            nextActions = if (result.terminal) {
                emptySet()
            } else {
                result.nextActions.ifEmpty { setOf(requireNotNull(result.nextAction)) }
            },
            errorCode = result.errorCode ?: "AUTHENTICATION_FAILED",
        )
    }

    private fun isPendingFederatedMfa(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
    ): Boolean = transaction.status == AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED &&
        transaction.method?.startsWith(METHOD_EXTERNAL_PREFIX) == true &&
        transaction.userId != null && transaction.acr == ACR_FEDERATED && transaction.context == null &&
        action in LOCAL_SECOND_FACTOR_ACTIONS

    private fun isPendingPluggableSecondFactor(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
    ): Boolean = transaction.purpose == AuthenticationTransactionPurposeEnum.LOGIN &&
        transaction.status == AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED &&
        transaction.userId != null && transaction.tenantId != null && transaction.acr != null &&
        transaction.context == null && secondFactorRegistry.supports(action)

    private fun applyFederatedSecondFactor(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): AuthenticationTransaction {
        val userId = requireNotNull(transaction.userId)
        val tenantId = requireNotNull(transaction.tenantId)
        val verifier = requireNotNull(federatedSecondFactorVerifier) {
            "Federated second-factor verifier is not available"
        }
        val result = verifier.verify(userId, tenantId, requireNotNull(transaction.username), action, request)
        return applySecondFactorResult(transaction, result)
    }

    private fun applyPluggableSecondFactor(
        transaction: AuthenticationTransaction,
        action: AuthenticationActionEnum,
        request: AuthenticationActionRequest,
    ): AuthenticationTransaction {
        val result = secondFactorRegistry.verify(
            action = action,
            transactionId = transaction.id,
            userId = requireNotNull(transaction.userId),
            tenantId = requireNotNull(transaction.tenantId),
            request = request,
        )
        return applySecondFactorResult(transaction, result)
    }

    private fun applySecondFactorResult(
        transaction: AuthenticationTransaction,
        result: SecondFactorVerificationResult,
    ): AuthenticationTransaction {
        if (!result.success) {
            return transaction.copy(
                status = AuthenticationTransactionStatusEnum.CHALLENGE_REQUIRED,
                errorCode = result.errorCode ?: "SECOND_FACTOR_FAILED",
            )
        }
        val now = Instant.now()
        val amr = transaction.amr + requireNotNull(result.method)
        val acr = result.acr ?: DefaultAuthenticationAssurancePolicy.ACR_MFA
        val userId = requireNotNull(transaction.userId)
        val tenantId = requireNotNull(transaction.tenantId)
        return transaction.copy(
            status = AuthenticationTransactionStatusEnum.COMPLETED,
            nextActions = emptySet(),
            amr = amr,
            acr = acr,
            context = AuthenticationContext(
                userId = userId,
                tenantId = tenantId,
                authTime = now,
                amr = amr,
                acr = acr,
            ),
            errorCode = null,
            postAuthenticationActions = emptySet(),
        )
    }

    /**
     * The single write point for transaction state, and therefore where the outcome audit is fed.
     *
     * Auditing here rather than at each outcome site is what makes the feed complete: every path — password,
     * TOTP, recovery code, Passkey, federated callback, step-up — reaches a terminal state through this method,
     * and a path added later cannot forget to report itself. The store's compare-and-set makes the transition
     * happen once, so the row is written once too.
     */
    private fun save(
        current: AuthenticationTransaction,
        updated: AuthenticationTransaction,
        observation: AuthLoginEventObservation = AuthLoginEventObservation.EMPTY,
    ): AuthenticationTransaction {
        val saved = requireNotNull(store.save(updated, current.version)) {
            "Authentication transaction was modified concurrently: ${current.id}"
        }
        if (!current.isTerminal()) recordOutcome(saved, observation)
        return saved
    }

    /**
     * Records completions and failures only.
     *
     * A cancelled or expired transaction is somebody walking away from a form, not an authentication outcome;
     * recording it would fill the audit with rows that carry no decision and no failure to investigate.
     */
    private fun recordOutcome(
        transaction: AuthenticationTransaction,
        observation: AuthLoginEventObservation,
    ) {
        val success = when (transaction.status) {
            AuthenticationTransactionStatusEnum.COMPLETED -> true
            AuthenticationTransactionStatusEnum.FAILED -> false
            else -> return
        }
        loginEventService?.record(
            AuthLoginEventRecordCommand(
                transactionId = transaction.id,
                purpose = transaction.purpose,
                success = success,
                tenantId = transaction.tenantId,
                userId = transaction.userId,
                // The attempted name is kept for failures, where it is often the only thing identifying the
                // attempt; the service digests it, so nothing readable is stored either way.
                identifier = transaction.username,
                providerId = transaction.method?.takeIf { it.startsWith(METHOD_EXTERNAL_PREFIX) }
                    ?.removePrefix(METHOD_EXTERNAL_PREFIX),
                authenticationMethod = transaction.method,
                failureCode = if (success) null else transaction.errorCode ?: "AUTHENTICATION_FAILED",
                acr = transaction.acr,
                amr = transaction.amr,
                observation = observation,
                occurredAt = LocalDateTime.ofInstant(transaction.updatedAt, ZoneOffset.UTC),
            )
        )
    }

    private fun AuthenticationActionRequest.toObservation() = AuthLoginEventObservation(
        loginIp = loginIp,
        loginDevice = loginDevice,
        loginBrowser = loginBrowser,
        loginOs = loginOs,
        userAgent = userAgent,
    )

    private fun externalMethod(providerId: String): String = "$METHOD_EXTERNAL_PREFIX${providerId.trim()}"

    private fun availableFederatedSecondFactors(
        userId: String,
        tenantId: String,
        allowedMethods: Set<MfaMethodEnum>,
    ): Set<AuthenticationActionEnum> = buildSet {
        if (MfaMethodEnum.TOTP in allowedMethods && federatedSecondFactorVerifier != null) {
            addAll(federatedSecondFactorVerifier.availableActions(userId, tenantId))
        }
        addAll(secondFactorRegistry.availableActions(userId, tenantId, allowedMethods))
    }

    companion object {
        private const val METHOD_EXTERNAL_PREFIX = "external:"
        private const val METHOD_FEDERATED = "federated"
        private const val ACR_FEDERATED = "urn:kudos:acr:federated"
        private val LOCAL_SECOND_FACTOR_ACTIONS = setOf(
            AuthenticationActionEnum.VERIFY_TOTP,
            AuthenticationActionEnum.VERIFY_RECOVERY_CODE,
        )
    }
}
