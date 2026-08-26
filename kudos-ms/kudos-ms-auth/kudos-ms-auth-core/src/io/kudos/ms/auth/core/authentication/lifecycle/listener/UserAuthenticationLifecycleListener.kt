package io.kudos.ms.auth.core.authentication.lifecycle.listener

import io.kudos.ms.auth.core.authentication.lifecycle.service.iservice.IAuthenticationLifecycleService
import io.kudos.ms.user.core.account.event.UserAccountBatchDeleted
import io.kudos.ms.user.core.account.event.UserAccountDeleted
import io.kudos.ms.user.core.account.event.UserAuthenticationInvalidated
import org.springframework.stereotype.Component
import org.springframework.transaction.event.TransactionPhase
import org.springframework.transaction.event.TransactionalEventListener

/** Converts committed user-account security changes into a complete authentication invalidation. */
@Component
open class UserAuthenticationLifecycleListener(
    private val lifecycleService: IAuthenticationLifecycleService,
) {

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAuthenticationInvalidated) {
        lifecycleService.invalidateAll(event.tenantId, event.id, event.reason.name)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountDeleted) {
        lifecycleService.invalidateAll(event.tenantId, event.id, ACCOUNT_DELETED)
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    open fun on(event: UserAccountBatchDeleted) {
        event.items.forEach {
            lifecycleService.invalidateAll(it.tenantId, it.id, ACCOUNT_DELETED)
        }
    }

    private companion object {
        const val ACCOUNT_DELETED = "ACCOUNT_DELETED"
    }
}
