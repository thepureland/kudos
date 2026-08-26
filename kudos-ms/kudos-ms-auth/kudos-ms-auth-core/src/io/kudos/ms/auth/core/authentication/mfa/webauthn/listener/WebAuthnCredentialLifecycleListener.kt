package io.kudos.ms.auth.core.authentication.mfa.webauthn.listener

import io.kudos.ms.auth.core.authentication.mfa.webauthn.dao.AuthWebAuthnCredentialDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** Removes orphaned public-key credentials after their owning account is deleted. */
@Component
open class WebAuthnCredentialLifecycleListener(
    private val dao: AuthWebAuthnCredentialDao,
) {

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
