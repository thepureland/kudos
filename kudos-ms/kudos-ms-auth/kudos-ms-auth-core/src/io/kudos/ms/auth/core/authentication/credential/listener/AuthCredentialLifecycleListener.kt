package io.kudos.ms.auth.core.authentication.credential.listener

import io.kudos.base.logger.LogFactory
import io.kudos.ms.auth.core.authentication.credential.dao.AuthCredentialDao
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Propagation
import org.springframework.transaction.annotation.Transactional
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/**
 * Removes an account's credentials once the account itself is gone.
 *
 * Deleted outright rather than revoked: a revoked credential is kept as evidence of what a live account had,
 * and there is no account left for these to be evidence about. This mirrors what the retired-password history
 * already does on the same events.
 */
@Component
open class AuthCredentialLifecycleListener(
    private val dao: AuthCredentialDao,
) {

    private val log = LogFactory.getLog(this::class)

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted) {
        purge(event.tenantId, event.id)
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.items.forEach { purge(it.tenantId, it.id) }
    }

    /** Logged because credentials disappearing is security-relevant and otherwise leaves no trace. */
    private fun purge(tenantId: String, userId: String) {
        val purged = dao.deleteByUser(tenantId, userId)
        if (purged > 0) {
            log.info("Purged $purged credential(s) of deleted account $userId in tenant $tenantId")
        }
    }
}
