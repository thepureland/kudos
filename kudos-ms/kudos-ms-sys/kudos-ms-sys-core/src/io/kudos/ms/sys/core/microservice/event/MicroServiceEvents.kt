package io.kudos.ms.sys.core.microservice.event

/**
 * 微服务（`sys_micro_service`）领域事件。`id` 即 code。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysMicroServiceEvent {
    val id: String
}

data class SysMicroServiceInserted(override val id: String) : SysMicroServiceEvent
data class SysMicroServiceUpdated(override val id: String) : SysMicroServiceEvent
data class SysMicroServiceDeleted(override val id: String) : SysMicroServiceEvent
data class SysMicroServiceBatchDeleted(val ids: Collection<String>) : SysMicroServiceEvent {
    override val id: String get() = ids.first()
}
