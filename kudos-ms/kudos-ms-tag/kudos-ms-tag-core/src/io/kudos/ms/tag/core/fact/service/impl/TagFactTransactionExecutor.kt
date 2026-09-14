package io.kudos.ms.tag.core.fact.service.impl

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

/**
 * Opens a transaction around a caller-supplied unit of fact work.
 *
 * Keeping this boundary in a separate Spring bean avoids self-invocation bypassing the transaction
 * proxy when the public batch API processes bounded slices.
 */
@Component
open class TagFactTransactionExecutor {
    @Transactional(rollbackFor = [Exception::class])
    open fun <T> execute(block: () -> T): T = block()
}
