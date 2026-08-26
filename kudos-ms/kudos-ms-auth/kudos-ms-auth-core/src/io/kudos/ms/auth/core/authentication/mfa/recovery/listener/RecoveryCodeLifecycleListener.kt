package io.kudos.ms.auth.core.authentication.mfa.recovery.listener

import io.kudos.ms.auth.core.authentication.mfa.recovery.dao.AuthRecoveryCodeDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener
import java.time.LocalDateTime
import java.time.ZoneOffset

/** Revokes codes when the owning authenticator changes and removes them with the account. */
@Component
open class RecoveryCodeLifecycleListener(
    private val dao: AuthRecoveryCodeDao,
) {

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAuthenticationInvalidated) {
        if (event.reason == UserAuthenticationInvalidated.Reason.AUTHENTICATOR_CHANGED) {
            dao.revokeActive(event.tenantId, event.id, LocalDateTime.now(ZoneOffset.UTC))
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted) {
        dao.deleteByUserId(event.id)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.items.forEach { dao.deleteByUserId(it.id) }
    }
}
