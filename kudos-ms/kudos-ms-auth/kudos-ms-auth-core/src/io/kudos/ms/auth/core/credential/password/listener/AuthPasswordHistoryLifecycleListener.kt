package io.kudos.ms.auth.core.credential.password.listener

import io.kudos.ms.auth.core.credential.password.dao.AuthPasswordHistoryDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** Removes retired credential material after its owning account is deleted. */
@Component
open class AuthPasswordHistoryLifecycleListener(
    private val dao: AuthPasswordHistoryDao,
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
