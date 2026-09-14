package io.kudos.ms.tag.core.assignment.service.impl

import org.springframework.stereotype.Component
import org.springframework.transaction.annotation.Transactional

@Component
open class TagAssignmentTransactionExecutor {
    @Transactional(rollbackFor = [Exception::class])
    open fun <T> execute(block: () -> T): T = block()
}
