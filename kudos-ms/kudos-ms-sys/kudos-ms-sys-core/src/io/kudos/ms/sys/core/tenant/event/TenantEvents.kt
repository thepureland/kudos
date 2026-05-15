package io.kudos.ms.sys.core.tenant.event

/**
 * 租户（`sys_tenant`）领域事件。由 [Spring `@TransactionalEventListener(AFTER_COMMIT)`][org.springframework.transaction.event.TransactionalEventListener]
 * 在事务提交后派发，避免事务回滚时污染缓存。
 *
 * 设计与 access rule 域的同类事件对齐（见 P1 PoC），订阅方仅依据事件携带的 id 反查最新行或清除缓存，不再
 * 直接由 service 调用 `cache.syncOnX(...)`。
 *
 * @author K
 * @author AI: Cursor
 * @since 1.0.0
 */
sealed interface SysTenantEvent {
    val id: String
}

/** 新增完成事件。 */
data class SysTenantInserted(override val id: String) : SysTenantEvent

/** 更新完成事件（含 `updateActive` 与一般 update）。 */
data class SysTenantUpdated(override val id: String) : SysTenantEvent

/** 删除完成事件。 */
data class SysTenantDeleted(override val id: String) : SysTenantEvent

/** 批量删除完成事件。 */
data class SysTenantBatchDeleted(val ids: Collection<String>) : SysTenantEvent {
    override val id: String get() = ids.first()
}
